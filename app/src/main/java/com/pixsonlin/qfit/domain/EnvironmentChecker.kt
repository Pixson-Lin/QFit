package com.pixsonlin.qfit.domain

import android.Manifest
import android.app.AlarmManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

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
     * Open this app's battery usage / unrestricted page when possible.
     * Tries several deep links before falling back to app-info (where user
     * must tap「電池」again).
     */
    fun openAppBatterySettings() {
        val pkg = context.packageName
        val packageUri = Uri.parse("package:$pkg")
        val candidates = buildList {
            // AOSP / Pixel: per-app battery usage (不受限制 / 最佳化 / 受限制)
            add(
                Intent(ACTION_APP_BATTERY_SETTINGS).apply {
                    data = packageUri
                    addCategory(Intent.CATEGORY_DEFAULT)
                },
            )
            add(
                Intent(ACTION_APP_BATTERY_SETTINGS).apply {
                    putExtra(Intent.EXTRA_PACKAGE_NAME, pkg)
                    putExtra(Settings.EXTRA_APP_PACKAGE, pkg)
                    addCategory(Intent.CATEGORY_DEFAULT)
                },
            )
            // Older / alternate AOSP action for advanced power usage detail
            add(
                Intent(ACTION_VIEW_ADVANCED_POWER_USAGE_DETAIL).apply {
                    data = packageUri
                    addCategory(Intent.CATEGORY_DEFAULT)
                },
            )
            add(
                Intent(ACTION_VIEW_ADVANCED_POWER_USAGE_DETAIL).apply {
                    putExtra("package", pkg)
                    putExtra("extra_package_name", pkg)
                    putExtra(Intent.EXTRA_PACKAGE_NAME, pkg)
                    addCategory(Intent.CATEGORY_DEFAULT)
                },
            )
            // Settings activity class used on some AOSP builds
            add(
                Intent().apply {
                    component = ComponentName(
                        "com.android.settings",
                        "com.android.settings.fuelgauge.AdvancedPowerUsageDetail",
                    )
                    putExtra("package", pkg)
                    putExtra("extra_package_name", pkg)
                    putExtra(Intent.EXTRA_PACKAGE_NAME, pkg)
                },
            )
            addAll(oemAppBatteryIntents(pkg))
            add(appDetailsIntent())
        }
        for (intent in candidates) {
            if (tryStart(intent)) return
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

    private fun tryStart(intent: Intent): Boolean =
        runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }.getOrDefault(false)

    private fun oemAppBatteryIntents(pkg: String): List<Intent> {
        val maker = Build.MANUFACTURER.orEmpty().lowercase()
        return buildList {
            if (maker.contains("samsung")) {
                add(
                    Intent().apply {
                        component = ComponentName(
                            "com.samsung.android.lool",
                            "com.samsung.android.sm.battery.ui.BatteryActivity",
                        )
                    },
                )
                add(
                    Intent().apply {
                        component = ComponentName(
                            "com.samsung.android.sm",
                            "com.samsung.android.sm.ui.battery.BatteryActivity",
                        )
                    },
                )
            }
            if (
                maker.contains("xiaomi") ||
                maker.contains("redmi") ||
                maker.contains("poco") ||
                maker.contains("blackshark")
            ) {
                add(
                    Intent("miui.intent.action.POWER_HIDE_MODE_APP_LIST").apply {
                        putExtra("package_name", pkg)
                        putExtra("package_label", "QFit")
                    },
                )
                add(
                    Intent().apply {
                        component = ComponentName(
                            "com.miui.powerkeeper",
                            "com.miui.powerkeeper.ui.HiddenAppsConfigActivity",
                        )
                        putExtra("package_name", pkg)
                        putExtra("package_label", "QFit")
                    },
                )
            }
        }
    }

    companion object {
        private const val ACTION_APP_BATTERY_SETTINGS = "android.settings.APP_BATTERY_SETTINGS"
        private const val ACTION_VIEW_ADVANCED_POWER_USAGE_DETAIL =
            "android.settings.VIEW_ADVANCED_POWER_USAGE_DETAIL"
    }
}
