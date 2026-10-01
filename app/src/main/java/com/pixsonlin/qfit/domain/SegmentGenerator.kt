package com.pixsonlin.qfit.domain

import com.pixsonlin.qfit.data.IntensityLevel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.round
import kotlin.math.sqrt
import kotlin.random.Random

class SegmentGenerator(
    private val random: Random = Random.Default,
) {
    fun nextDurationSeconds(): Int = random.nextInt(MIN_DURATION_SEC, MAX_DURATION_SEC_EXCLUSIVE)

    fun generate(
        index: Int,
        startMillis: Long,
        level: IntensityLevel,
        durationSec: Int = nextDurationSeconds(),
    ): SegmentData {
        val baseSteps = level.cadenceSpm / 60.0 * durationSec
        val steps = max(MIN_STEPS, gaussianRound(mean = baseSteps, sigma = STEP_SIGMA, random = random))
        val distance = (steps * level.strideMeters).roundToTwoDecimals().toFloat()
        return SegmentData(
            segmentIndex = index,
            startTimeMillis = startMillis,
            endTimeMillis = startMillis + durationSec * 1_000L,
            steps = steps,
            distanceMeters = distance,
        )
    }

    companion object {
        const val MIN_DURATION_SEC = 25
        const val MAX_DURATION_SEC_EXCLUSIVE = 36
        const val MIN_STEPS = 1
        const val STEP_SIGMA = 5.0
    }
}

private fun gaussianRound(mean: Double, sigma: Double, random: Random): Int {
    val u1 = random.nextDouble().coerceAtLeast(Double.MIN_VALUE)
    val u2 = random.nextDouble()
    val z = sqrt(-2.0 * ln(u1)) * cos(2.0 * PI * u2)
    return round(mean + sigma * z).toInt()
}

private fun Double.roundToTwoDecimals(): Double = round(this * 100.0) / 100.0
