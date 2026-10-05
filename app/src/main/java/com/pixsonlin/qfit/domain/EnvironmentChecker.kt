package com.pixsonlin.qfit.domain

import android.Manifest
import android.app.AlarmManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
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

    /**
     * Open Settings → Apps → QFit → Battery (三選一：不受限制 / 最佳化 / 受限).
     * Confirmed on owner device as SubSettings + PowerBackgroundUsageDetail
     * with extra_package_name=com.pixsonlin.qfit.
     */
    fun openAppBatterySettings() {
        val pkg = context.packageName
        val uid = runCatching {
            context.packageManager.getPackageUid(pkg, 0)
        }.getOrDefault(-1)
        val packageUri = Uri.parse("package:$pkg")
        val fragmentArgs = Bundle().apply {
            putString(EXTRA_PACKAGE_NAME, pkg)
            if (uid >= 0) putInt(EXTRA_UID, uid)
            putBoolean(EXTRA_SHOW_TIME_INFO, false)
            putString(EXTRA_POWER_USAGE_PERCENT, "0%")
        }

        val candidates = buildList {
            // Owner dump: PowerBackgroundUsageDetail via Settings SubSettings.
            for (fragment in POWER_BACKGROUND_USAGE_FRAGMENTS) {
                add(subSettingsIntent(fragment, fragmentArgs))
            }
            // AOSP trampoline used by APP_BATTERY_SETTINGS on many builds.
            add(
                Intent().apply {
                    component = ComponentName(
                        "com.android.settings",
                        "com.android.settings.fuelgauge.AdvancedPowerUsageDetailActivity",
                    )
                    data = packageUri
                },
            )
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
            // Last resort: app info (user taps「電池」).
            add(appDetailsIntent())
        }

        for (intent in candidates) {
            if (!canResolve(intent)) {
                Log.d(
                    TAG,
                    "skip unresolved battery intent action=${intent.action} " +
                        "component=${intent.component} fragment=${intent.getStringExtra(EXTRA_SHOW_FRAGMENT)}",
                )
                continue
            }
            if (tryStart(intent)) {
                Log.d(
                    TAG,
                    "started battery intent action=${intent.action} " +
                        "component=${intent.component} fragment=${intent.getStringExtra(EXTRA_SHOW_FRAGMENT)}",
                )
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

    private fun subSettingsIntent(fragmentClass: String, args: Bundle): Intent =
        Intent(Intent.ACTION_MAIN).apply {
            setClassName("com.android.settings", "com.android.settings.SubSettings")
            putExtra(EXTRA_SHOW_FRAGMENT, fragmentClass)
            putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, args)
            // Some builds require a metrics category; harmless if ignored.
            putExtra(EXTRA_SOURCE_METRICS_CATEGORY, METRICS_INSTALLED_APP_DETAILS)
        }

    private fun canResolve(intent: Intent): Boolean =
        intent.resolveActivity(context.packageManager) != null

    private fun tryStart(intent: Intent): Boolean =
        runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }.onFailure {
            Log.w(
                TAG,
                "startActivity failed action=${intent.action} component=${intent.component} " +
                    "fragment=${intent.getStringExtra(EXTRA_SHOW_FRAGMENT)}",
                it,
            )
        }.getOrDefault(false)

    companion object {
        private const val TAG = "QFit_Env"
        private const val ACTION_APP_BATTERY_SETTINGS = "android.settings.APP_BATTERY_SETTINGS"
        private const val ACTION_VIEW_ADVANCED_POWER_USAGE_DETAIL =
            "android.settings.VIEW_ADVANCED_POWER_USAGE_DETAIL"

        private const val EXTRA_SHOW_FRAGMENT = ":settings:show_fragment"
        private const val EXTRA_SHOW_FRAGMENT_ARGUMENTS = ":settings:show_fragment_args"
        private const val EXTRA_SOURCE_METRICS_CATEGORY = ":settings:source_metrics"
        private const val METRICS_INSTALLED_APP_DETAILS = 20

        private const val EXTRA_PACKAGE_NAME = "extra_package_name"
        private const val EXTRA_UID = "extra_uid"
        private const val EXTRA_SHOW_TIME_INFO = "extra_show_time_info"
        private const val EXTRA_POWER_USAGE_PERCENT = "extra_power_usage_percent"

        /** Candidate FQCNs for the three-option app battery page. */
        private val POWER_BACKGROUND_USAGE_FRAGMENTS = listOf(
            "com.android.settings.fuelgauge.PowerBackgroundUsageDetail",
            "com.android.settings.fuelgauge.batteryusage.PowerBackgroundUsageDetail",
            "com.samsung.android.settings.fuelgauge.PowerBackgroundUsageDetail",
            "com.samsung.android.settings.battery.PowerBackgroundUsageDetail",
            "com.android.settings.fuelgauge.AdvancedPowerUsageDetail",
        )
    }
}
