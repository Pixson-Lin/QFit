package com.pixsonlin.qfit.domain

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat

data class EnvironmentStatus(
    val healthConnectReady: Boolean,
    val notificationsReady: Boolean,
    val batteryReady: Boolean,
    val exactAlarmReady: Boolean,
)

class EnvironmentChecker(private val context: Context) {
    suspend fun status(): EnvironmentStatus = EnvironmentStatus(
        healthConnectReady = HealthConnectWriter.hasWritePermission(context),
        notificationsReady = areNotificationsEnabled(),
        batteryReady = isBatteryOptimizationDisabled(),
        exactAlarmReady = canScheduleExactAlarms(),
    )

    fun areNotificationsEnabled(): Boolean {
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun isBatteryOptimizationDisabled(): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun canScheduleExactAlarms(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

    fun batteryOptimizationIntent(): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }

    /**
     * When already exempt, open this app's battery / details page instead of the
     * system-wide optimization list (that list often hides already-unrestricted apps).
     */
    fun appBatteryOrDetailsIntent(): Intent {
        val packageUri = Uri.parse("package:${context.packageName}")
        val appBattery = Intent("android.settings.APP_BATTERY_SETTINGS").apply {
            data = packageUri
        }
        return if (appBattery.resolveActivity(context.packageManager) != null) {
            appBattery
        } else {
            appDetailsIntent()
        }
    }

    fun exactAlarmIntent(): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            appDetailsIntent()
        }

    fun notificationSettingsIntent(): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }

    fun appDetailsIntent(): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
}
