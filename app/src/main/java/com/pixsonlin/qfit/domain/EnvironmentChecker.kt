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
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

data class EnvironmentStatus(
    val healthConnectReady: Boolean,
    val notificationsReady: Boolean,
    val batteryReady: Boolean,
    val exactAlarmReady: Boolean,
)

class EnvironmentChecker(private val context: Context) {
    suspend fun status(): EnvironmentStatus {
        val status = EnvironmentStatus(
            healthConnectReady = HealthConnectWriter.hasWritePermission(context),
            notificationsReady = areNotificationsEnabled(),
            batteryReady = isBatteryOptimizationDisabled(),
            exactAlarmReady = canScheduleExactAlarms(),
        )
        // Platform note: canScheduleExactAlarms() is also true when the app is on the
        // battery/power allowlist — so「計時」often tracks「電池最佳化」even though
        // they are different settings UIs.
        Log.d(
            TAG,
            "env battery=${status.batteryReady} exactAlarm=${status.exactAlarmReady} " +
                "hc=${status.healthConnectReady} notif=${status.notificationsReady} " +
                "maker=${Build.MANUFACTURER} sdk=${Build.VERSION.SDK_INT}",
        )
        return status
    }

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
     * Open this app's battery usage page when possible.
     * Only starts intents that resolve; skips known flash-and-finish paths on OEMs.
     */
    fun openAppBatterySettings() {
        val pkg = context.packageName
        val packageUri = Uri.parse("package:$pkg")
        val maker = Build.MANUFACTURER.orEmpty().lowercase()
        val candidates = buildList {
            // Samsung One UI: AOSP APP_BATTERY_SETTINGS often starts then immediately
            // finishes (screen flash). Prefer app-info / Device Care battery.
            if (!maker.contains("samsung")) {
                add(
                    Intent(ACTION_APP_BATTERY_SETTINGS).apply {
                        data = packageUri
                        addCategory(Intent.CATEGORY_DEFAULT)
                    },
                )
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
            }
            addAll(oemAppBatteryIntents(pkg, maker))
            add(appDetailsIntent())
        }
        for (intent in candidates) {
            if (!canResolve(intent)) {
                Log.d(TAG, "skip unresolved battery intent action=${intent.action} component=${intent.component}")
                continue
            }
            if (tryStart(intent)) {
                Log.d(TAG, "started battery intent action=${intent.action} component=${intent.component}")
                return
            }
        }
        Log.w(TAG, "no battery settings intent worked; last resort app details")
        tryStart(appDetailsIntent())
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

    private fun canResolve(intent: Intent): Boolean =
        intent.resolveActivity(context.packageManager) != null

    private fun tryStart(intent: Intent): Boolean =
        runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }.onFailure {
            Log.w(TAG, "startActivity failed action=${intent.action} component=${intent.component}", it)
        }.getOrDefault(false)

    private fun oemAppBatteryIntents(pkg: String, maker: String): List<Intent> = buildList {
        if (maker.contains("samsung")) {
            // Device Care battery hub (may still not be per-app; better than flash).
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

    companion object {
        private const val TAG = "QFit_Env"
        private const val ACTION_APP_BATTERY_SETTINGS = "android.settings.APP_BATTERY_SETTINGS"
        private const val ACTION_VIEW_ADVANCED_POWER_USAGE_DETAIL =
            "android.settings.VIEW_ADVANCED_POWER_USAGE_DETAIL"
    }
}
