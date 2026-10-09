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
    fun nextDurationSeconds(shortRun: Boolean = false): Int {
        val minSec = if (shortRun) SHORT_MIN_DURATION_SEC else MIN_DURATION_SEC
        val maxExclusive =
            if (shortRun) SHORT_MAX_DURATION_SEC_EXCLUSIVE else MAX_DURATION_SEC_EXCLUSIVE
        return random.nextInt(minSec, maxExclusive)
    }

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
        /** Default segment length range (inclusive min … exclusive max). */
        const val MIN_DURATION_SEC = 25
        const val MAX_DURATION_SEC_EXCLUSIVE = 36

        /**
         * Short-run segment length (runs ≤ [SHORT_RUN_THRESHOLD_MINUTES] min).
         * Shorter slices so steps appear sooner on 1–5 min sessions.
         */
        const val SHORT_MIN_DURATION_SEC = 10
        const val SHORT_MAX_DURATION_SEC_EXCLUSIVE = 16
        const val SHORT_RUN_THRESHOLD_MINUTES = 5

        /**
         * Last segment may end up to this many seconds past the configured session end.
         * Worst case: prior segment ends at sessionEnd − 1s, last draw is 35s → +34s.
         */
        const val LAST_SEGMENT_OVERRUN_SEC = 34

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
