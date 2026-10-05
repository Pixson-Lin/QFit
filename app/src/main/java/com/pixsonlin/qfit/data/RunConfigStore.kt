package com.pixsonlin.qfit.data

import android.content.Context
import kotlin.math.abs

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

    /** First-launch default is 超慢跑 when nothing has been saved yet. */
    fun getIntensityOrDefault(): IntensityLevel =
        getIntensityOrNull() ?: DEFAULT_INTENSITY

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
        const val MIN_DURATION_MIN = 1
        const val MAX_DURATION_MIN = 240
        const val DEFAULT_DURATION_MIN = 20
        const val DEFAULT_BACKGROUND_RUN = true
        val DEFAULT_INTENSITY: IntensityLevel = IntensityLevel.SUPER_SLOW_JOG

        /**
         * Allowed duration stops (minutes), shown with equal visual spacing on the slider.
         *
         * 1, 3, 5, then +5 to 60, +10 to 120, +20 to 240 → 26 stops.
         */
        val DURATION_STOPS: List<Int> = buildList {
            add(1)
            add(3)
            add(5)
            for (m in 10..60 step 5) add(m)
            for (m in 70..120 step 10) add(m)
            for (m in 140..240 step 20) add(m)
        }

        private const val PREFS = "qfit_run_config"
        private const val KEY_DURATION_MIN = "duration_minutes"
        private const val KEY_INTENSITY = "intensity"
        private const val KEY_BACKGROUND_RUN = "background_run"

        fun snapToStep(minutes: Int): Int =
            DURATION_STOPS.minBy { abs(it - minutes) }
    }
}
