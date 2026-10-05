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
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

data class EnvironmentStatus(
    val healthConnectReady: Boolean,
    val notificationsReady: Boolean,
    val batteryReady: Boolean,
    val exactAlarmReady: Boolean,
)

/** Result of trying to open the per-app battery page. */
enum class BatterySettingsOpenResult {
    /** Likely landed on a battery-related page. */
    OPENED_BATTERY_PAGE,

    /**
     * Opened app info (Settings → Apps → QFit). User must tap「電池」
     * for the three-option page. Common on Samsung: SubSettings is not
     * exported, and AdvancedPowerUsageDetailActivity crashes.
     */
    OPENED_APP_DETAILS,

    FAILED,
}

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
     * Best-effort open of the per-app Battery page
     * (不受限制 / 最佳化 / 受限).
     *
     * Samsung One UI (owner dump): `SubSettings` + `PowerBackgroundUsageDetail`
     * is **not exported** to third-party apps. The public trampoline
     * `AdvancedPowerUsageDetailActivity` crashes on resume (NPE in
     * AppButtonsPreferenceController). So Samsung falls back to app info.
     */
    fun openAppBatterySettings(): BatterySettingsOpenResult {
        val pkg = context.packageName
        val packageUri = Uri.fromParts("package", pkg, null)
        val maker = Build.MANUFACTURER.orEmpty().lowercase()
        val samsung = maker.contains("samsung")

        // Do NOT try SubSettings — not exported (SecurityException).
        // Do NOT try AdvancedPowerUsageDetailActivity on Samsung — crashes
        // Settings (flash and return). Owner log 2026-10-05.
        if (!samsung) {
            val batteryCandidates = listOf(
                Intent(ACTION_APP_BATTERY_SETTINGS).apply {
                    data = packageUri
                    addCategory(Intent.CATEGORY_DEFAULT)
                    putExtra(EXTRA_PACKAGE_NAME, pkg)
                    putExtra(Intent.EXTRA_PACKAGE_NAME, pkg)
                },
                Intent(ACTION_VIEW_ADVANCED_POWER_USAGE_DETAIL).apply {
                    data = packageUri
                    addCategory(Intent.CATEGORY_DEFAULT)
                    putExtra(EXTRA_PACKAGE_NAME, pkg)
                    putExtra(Intent.EXTRA_PACKAGE_NAME, pkg)
                },
            )
            for (intent in batteryCandidates) {
                if (!canResolve(intent)) {
                    Log.d(TAG, "skip unresolved battery intent action=${intent.action}")
                    continue
                }
                if (tryStart(intent)) {
                    Log.d(TAG, "started battery intent action=${intent.action} data=${intent.data}")
                    return BatterySettingsOpenResult.OPENED_BATTERY_PAGE
                }
            }
        } else {
            Log.d(TAG, "samsung: skip APP_BATTERY_SETTINGS trampoline (known crash)")
        }

        // Stable path: app info. Highlight「電池」when the OEM supports it.
        for (key in BATTERY_HIGHLIGHT_KEYS) {
            val details = appDetailsIntent().apply {
                putExtra(EXTRA_FRAGMENT_ARG_KEY, key)
                putExtra(":settings:show_fragment_args", android.os.Bundle().apply {
                    putString(EXTRA_FRAGMENT_ARG_KEY, key)
                })
            }
            if (tryStart(details)) {
                Log.d(TAG, "started app details with highlight key=$key")
                return BatterySettingsOpenResult.OPENED_APP_DETAILS
            }
        }
        if (tryStart(appDetailsIntent())) {
            Log.d(TAG, "started plain app details")
            return BatterySettingsOpenResult.OPENED_APP_DETAILS
        }
        Log.w(TAG, "failed to open any battery/app-details settings")
        return BatterySettingsOpenResult.FAILED
    }

    fun exactAlarmIntent(): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.fromParts("package", context.packageName, null)
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
            data = Uri.fromParts("package", context.packageName, null)
        }

    private fun canResolve(intent: Intent): Boolean =
        intent.resolveActivity(context.packageManager) != null

    private fun tryStart(intent: Intent): Boolean =
        runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }.onFailure {
            Log.w(TAG, "startActivity failed action=${intent.action} data=${intent.data}", it)
        }.getOrDefault(false)

    companion object {
        private const val TAG = "QFit_Env"
        private const val ACTION_APP_BATTERY_SETTINGS = "android.settings.APP_BATTERY_SETTINGS"
        private const val ACTION_VIEW_ADVANCED_POWER_USAGE_DETAIL =
            "android.settings.VIEW_ADVANCED_POWER_USAGE_DETAIL"
        private const val EXTRA_PACKAGE_NAME = "extra_package_name"
        private const val EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key"

        /** Preference keys OEMs may use for the Battery row in app info. */
        private val BATTERY_HIGHLIGHT_KEYS = listOf(
            "battery",
            "battery_settings",
            "app_battery_usage",
            "pref_app_battery_usage",
        )
    }
}
