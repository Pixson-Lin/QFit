package com.pixsonlin.qfit.data

import android.content.Context

class RunConfigStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getDurationMinutes(): Int =
        prefs.getInt(KEY_DURATION_MIN, DEFAULT_DURATION_MIN).coerceIn(MIN_DURATION_MIN, MAX_DURATION_MIN)

    fun setDurationMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_DURATION_MIN, minutes.coerceIn(MIN_DURATION_MIN, MAX_DURATION_MIN)).apply()
    }

    fun getIntensityOrNull(): IntensityLevel? {
        val name = prefs.getString(KEY_INTENSITY, null) ?: return null
        return runCatching { IntensityLevel.valueOf(name) }.getOrNull()
    }

    fun setIntensity(level: IntensityLevel?) {
        prefs.edit().apply {
            if (level == null) remove(KEY_INTENSITY) else putString(KEY_INTENSITY, level.name)
        }.apply()
    }

    companion object {
        const val MIN_DURATION_MIN = 5
        const val MAX_DURATION_MIN = 240
        const val DEFAULT_DURATION_MIN = 20
        private const val PREFS = "qfit_run_config"
        private const val KEY_DURATION_MIN = "duration_minutes"
        private const val KEY_INTENSITY = "intensity"
    }
}
