package com.pixsonlin.qfit.domain

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Length
import com.pixsonlin.qfit.data.IntensityLevel
import java.time.Instant
import java.time.ZoneId

object HealthConnectWriter {
    const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"

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

    /**
     * Play Store listing for the Health Connect provider app (needed on Android 13
     * and below when [sdkStatus] is [HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED]).
     */
    fun providerInstallIntent(): Intent =
        Intent(Intent.ACTION_VIEW).apply {
            setPackage("com.android.vending")
            data = Uri.parse(
                "market://details?id=$HEALTH_CONNECT_PACKAGE" +
                    "&url=healthconnect%3A%2F%2Fonboarding",
            )
            putExtra("overlay", true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun providerInstallWebIntent(): Intent =
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$HEALTH_CONNECT_PACKAGE"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Open Play Store (or web fallback) so the user can install Health Connect. */
    fun openProviderInstall(context: Context): Boolean {
        return try {
            context.startActivity(providerInstallIntent())
            true
        } catch (_: ActivityNotFoundException) {
            try {
                context.startActivity(providerInstallWebIntent())
                true
            } catch (_: ActivityNotFoundException) {
                false
            }
        }
    }

    /** Open HC settings when available; otherwise send the user to install HC. */
    fun openSettingsOrInstall(context: Context): Boolean {
        return when (sdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE -> {
                try {
                    context.startActivity(settingsIntent())
                    true
                } catch (_: ActivityNotFoundException) {
                    openProviderInstall(context)
                }
            }
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                openProviderInstall(context)
            else -> false
        }
    }

    suspend fun hasWritePermission(context: Context): Boolean {
        if (sdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) return false
        val granted = getClient(context).permissionController.getGrantedPermissions()
        return granted.containsAll(requiredPermissions)
    }

    /** Map QFit intensity → HC Int constant (not freeform). No title/notes. */
    fun exerciseTypeFor(intensity: IntensityLevel): Int = when (intensity) {
        IntensityLevel.STROLL -> ExerciseSessionRecord.EXERCISE_TYPE_WALKING
        IntensityLevel.SUPER_SLOW_JOG,
        IntensityLevel.JOG,
        IntensityLevel.MARATHON,
        IntensityLevel.SPRINT,
        -> ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
    }

    /**
     * Mid-run / catch-up: Steps only (one record per due segment).
     * Distance + Exercise are written once at session close via [writeSessionSummary].
     */
    suspend fun writeStepSegments(context: Context, segments: List<SegmentData>) {
        if (segments.isEmpty()) return
        val now = System.currentTimeMillis()
        segments.forEach { segment ->
            require(segment.startTimeMillis < segment.endTimeMillis)
            require(segment.endTimeMillis <= now) {
                "Segment end must not be in the future."
            }
        }
        val client = getClient(context)
        val zone = ZoneId.systemDefault()
        val records = mutableListOf<Record>()
        segments.forEach { segment ->
            val start = Instant.ofEpochMilli(segment.startTimeMillis)
            val end = Instant.ofEpochMilli(segment.endTimeMillis)
            val startOffset = zone.rules.getOffset(start)
            val endOffset = zone.rules.getOffset(end)
            records += StepsRecord(
                count = segment.steps.toLong(),
                startTime = start,
                endTime = end,
                startZoneOffset = startOffset,
                endZoneOffset = endOffset,
                metadata = Metadata.manualEntry(),
            )
        }
        client.insertRecords(records)
    }

    /**
     * Once per run at finalize: single Distance + single Exercise over the WRITTEN span.
     * [exerciseType] must be an HC `EXERCISE_TYPE_*` Int; title/notes are intentionally null.
     */
    suspend fun writeSessionSummary(
        context: Context,
        startTimeMillis: Long,
        endTimeMillis: Long,
        distanceMeters: Double,
        exerciseType: Int,
    ) {
        require(startTimeMillis < endTimeMillis)
        require(endTimeMillis <= System.currentTimeMillis()) {
            "Session end must not be in the future."
        }
        val client = getClient(context)
        val zone = ZoneId.systemDefault()
        val start = Instant.ofEpochMilli(startTimeMillis)
        val end = Instant.ofEpochMilli(endTimeMillis)
        val startOffset = zone.rules.getOffset(start)
        val endOffset = zone.rules.getOffset(end)
        val metadata = Metadata.manualEntry()
        client.insertRecords(
            listOf(
                DistanceRecord(
                    distance = Length.meters(distanceMeters),
                    startTime = start,
                    endTime = end,
                    startZoneOffset = startOffset,
                    endZoneOffset = endOffset,
                    metadata = metadata,
                ),
                ExerciseSessionRecord(
                    exerciseType = exerciseType,
                    startTime = start,
                    endTime = end,
                    startZoneOffset = startOffset,
                    endZoneOffset = endOffset,
                    metadata = metadata,
                ),
            ),
        )
    }
}
