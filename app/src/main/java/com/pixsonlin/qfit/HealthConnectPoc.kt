package com.pixsonlin.qfit

import android.content.Context
import android.content.Intent
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Metadata
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object HealthConnectPoc {
    const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"
    const val POC_STEP_COUNT = 188L

    val writeStepsPermission: String =
        HealthPermission.getWritePermission(StepsRecord::class)

    val requiredPermissions: Set<String> = setOf(writeStepsPermission)

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
     * Writes one on-device StepsRecord of [POC_STEP_COUNT] covering the last minute.
     * No Google account is used.
     */
    suspend fun writePocSteps(client: HealthConnectClient): StepsRecord {
        val end = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val start = end.minus(1, ChronoUnit.MINUTES)
        val zone = ZoneId.systemDefault()
        val startOffset = zone.rules.getOffset(start)
        val endOffset = zone.rules.getOffset(end)

        val record = StepsRecord(
            count = POC_STEP_COUNT,
            startTime = start,
            endTime = end,
            startZoneOffset = startOffset,
            endZoneOffset = endOffset,
            metadata = Metadata.manualEntry(),
        )
        client.insertRecords(listOf(record))
        return record
    }
}
