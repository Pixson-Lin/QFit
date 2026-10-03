package com.pixsonlin.qfit.data

import android.content.Context

class RunConfigStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getDurationMinutes(): Int =
        snapToStep(prefs.getInt(KEY_DURATION_MIN, DEFAULT_DURATION_MIN))

    fun setDurationMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_DURATION_MIN, snapToStep(minutes)).apply()
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

    /** When true, run uses a foreground notification so Android is less likely to kill it. */
    fun isBackgroundRunEnabled(): Boolean =
        prefs.getBoolean(KEY_BACKGROUND_RUN, DEFAULT_BACKGROUND_RUN)

    fun setBackgroundRunEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BACKGROUND_RUN, enabled).apply()
    }

    companion object {
        const val MIN_DURATION_MIN = 5
        const val MAX_DURATION_MIN = 240
        const val DURATION_STEP_MIN = 5
        const val DEFAULT_DURATION_MIN = 20
        const val DEFAULT_BACKGROUND_RUN = true
        private const val PREFS = "qfit_run_config"
        private const val KEY_DURATION_MIN = "duration_minutes"
        private const val KEY_INTENSITY = "intensity"
        private const val KEY_BACKGROUND_RUN = "background_run"

        fun snapToStep(minutes: Int): Int {
            val stepped =
                ((minutes + DURATION_STEP_MIN / 2) / DURATION_STEP_MIN) * DURATION_STEP_MIN
            return stepped.coerceIn(MIN_DURATION_MIN, MAX_DURATION_MIN)
        }

        /** Compose Slider `steps` = discrete values between ends exclusive. */
        fun sliderSteps(): Int =
            ((MAX_DURATION_MIN - MIN_DURATION_MIN) / DURATION_STEP_MIN) - 1
    }
}
