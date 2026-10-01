package com.pixsonlin.qfit.domain

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

    suspend fun hasWritePermission(context: Context): Boolean {
        if (sdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) return false
        val granted = getClient(context).permissionController.getGrantedPermissions()
        return granted.containsAll(requiredPermissions)
    }

    suspend fun writeSegments(context: Context, segments: List<SegmentData>) {
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
            val metadata = Metadata.manualEntry()
            records += StepsRecord(
                count = segment.steps.toLong(),
                startTime = start,
                endTime = end,
                startZoneOffset = startOffset,
                endZoneOffset = endOffset,
                metadata = metadata,
            )
            records += DistanceRecord(
                distance = Length.meters(segment.distanceMeters.toDouble()),
                startTime = start,
                endTime = end,
                startZoneOffset = startOffset,
                endZoneOffset = endOffset,
                metadata = metadata,
            )
            records += ExerciseSessionRecord(
                exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
                startTime = start,
                endTime = end,
                startZoneOffset = startOffset,
                endZoneOffset = endOffset,
                metadata = metadata,
            )
        }
        client.insertRecords(records)
    }
}
