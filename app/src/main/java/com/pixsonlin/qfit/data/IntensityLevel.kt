package com.pixsonlin.qfit.data

import androidx.annotation.StringRes
import com.pixsonlin.qfit.R

enum class IntensityLevel(
    @StringRes val labelRes: Int,
    val cadenceSpm: Int,
    val strideMeters: Double,
) {
    STROLL(R.string.intensity_stroll, 80, 0.60),
    SUPER_SLOW_JOG(R.string.intensity_super_slow_jog, 140, 0.67),
    JOG(R.string.intensity_jog, 165, 0.70),
    MARATHON(R.string.intensity_marathon, 180, 0.78),
    SPRINT(R.string.intensity_sprint, 210, 1.00),
}
