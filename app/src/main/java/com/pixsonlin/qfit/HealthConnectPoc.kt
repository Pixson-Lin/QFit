package com.pixsonlin.qfit

import android.content.Context
import android.content.Intent
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Length
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class PocSegment(
    val index: Int,
    val steps: Long,
    val distanceMeters: Double,
    val start: Instant,
    val end: Instant,
)

data class PocWriteResult(
    val segments: List<PocSegment>,
    val totalSteps: Long,
    val totalDistanceMeters: Double,
)

object HealthConnectPoc {
    const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"
    const val STEPS_PER_SEGMENT = 188L
    const val SHORT_RUN_SEGMENT_COUNT = 4
    const val SEGMENT_DURATION_SECONDS = 60L
    /** Rough stride used only for PoC distance (~0.7 m/step). */
    const val METERS_PER_STEP = 0.7

    private const val PREFS = "qfit_poc_write_cursor"
    private const val KEY_LAST_START = "last_start_epoch_sec"
    private const val KEY_LAST_END = "last_end_epoch_sec"

    val requiredPermissions: Set<String> = setOf(
        HealthPermission.getWritePermission(StepsRecord::class),
        HealthPermission.getWritePermission(DistanceRecord::class),
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
    )

    fun sdkStatus(context: Context): Int =
        HealthConnectClient.getSdkStatus(context, HEALTH_CONNECT_PACKAGE)

    fun getClient(context: Context): HealthConnectClient =
        HealthConnectClient.getOrCreate(context)

    fun settingsIntent(): Intent =
        Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun playStoreIntent(): Intent =
        Intent(
            Intent.ACTION_VIEW,
            android.net.Uri.parse("market://details?id=$HEALTH_CONNECT_PACKAGE"),
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    suspend fun hasWritePermission(client: HealthConnectClient): Boolean {
        val granted = client.permissionController.getGrantedPermissions()
        return granted.containsAll(requiredPermissions)
    }

    /**
     * Allocates [segmentCount] adjacent, non-overlapping 1-minute windows with end ≤ now.
     * Prefers appending after the previous block; if there is not enough room before now,
     * places the new block immediately before the previous block.
     */
    fun allocateSegments(context: Context, segmentCount: Int): List<PocSegment> {
        require(segmentCount > 0)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val nowSec = Instant.now().truncatedTo(ChronoUnit.SECONDS).epochSecond
        val lastStart = prefs.getLong(KEY_LAST_START, 0L)
        val lastEnd = prefs.getLong(KEY_LAST_END, 0L)
        val blockSeconds = SEGMENT_DURATION_SECONDS * segmentCount

        val startSec = when {
            lastEnd > 0L && lastEnd + blockSeconds <= nowSec -> lastEnd
            lastStart > 0L -> lastStart - blockSeconds
            else -> nowSec - blockSeconds
        }

        // Guard: never end in the future.
        val safeStart = if (startSec + blockSeconds > nowSec) {
            nowSec - blockSeconds
        } else {
            startSec
        }

        return (0 until segmentCount).map { index ->
            val segStart = Instant.ofEpochSecond(safeStart + index * SEGMENT_DURATION_SECONDS)
            val segEnd = segStart.plusSeconds(SEGMENT_DURATION_SECONDS)
            PocSegment(
                index = index,
                steps = STEPS_PER_SEGMENT,
                distanceMeters = STEPS_PER_SEGMENT * METERS_PER_STEP,
                start = segStart,
                end = segEnd,
            )
        }
    }

    private fun rememberBlock(context: Context, segments: List<PocSegment>) {
        val first = segments.first()
        val last = segments.last()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_START, first.start.epochSecond)
            .putLong(KEY_LAST_END, last.end.epochSecond)
            .apply()
    }

    /**
     * Writes steps + distance + exercise for each segment. No Google account is used.
     */
    suspend fun writeSegments(
        context: Context,
        client: HealthConnectClient,
        segmentCount: Int,
    ): PocWriteResult {
        val segments = allocateSegments(context, segmentCount)
        val zone = ZoneId.systemDefault()
        val records = mutableListOf<Record>()

        segments.forEach { segment ->
            val startOffset = zone.rules.getOffset(segment.start)
            val endOffset = zone.rules.getOffset(segment.end)
            val metadata = Metadata.manualEntry()
            records += StepsRecord(
                count = segment.steps,
                startTime = segment.start,
                endTime = segment.end,
                startZoneOffset = startOffset,
                endZoneOffset = endOffset,
                metadata = metadata,
            )
            records += DistanceRecord(
                distance = Length.meters(segment.distanceMeters),
                startTime = segment.start,
                endTime = segment.end,
                startZoneOffset = startOffset,
                endZoneOffset = endOffset,
                metadata = metadata,
            )
            records += ExerciseSessionRecord(
                exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
                startTime = segment.start,
                endTime = segment.end,
                startZoneOffset = startOffset,
                endZoneOffset = endOffset,
                metadata = metadata,
            )
        }

        client.insertRecords(records)
        rememberBlock(context, segments)

        return PocWriteResult(
            segments = segments,
            totalSteps = segments.sumOf { it.steps },
            totalDistanceMeters = segments.sumOf { it.distanceMeters },
        )
    }

    suspend fun writeOneSegment(context: Context, client: HealthConnectClient): PocWriteResult =
        writeSegments(context, client, segmentCount = 1)

    suspend fun writeShortRun(context: Context, client: HealthConnectClient): PocWriteResult =
        writeSegments(context, client, segmentCount = SHORT_RUN_SEGMENT_COUNT)
}
