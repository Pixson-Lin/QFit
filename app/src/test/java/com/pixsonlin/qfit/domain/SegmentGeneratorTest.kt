package com.pixsonlin.qfit.domain

import com.pixsonlin.qfit.data.IntensityLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.round
import kotlin.random.Random

class SegmentGeneratorTest {
    @Test
    fun nextDurationSeconds_defaultRange() {
        val generator = SegmentGenerator(Random(42))
        repeat(200) {
            val sec = generator.nextDurationSeconds(shortRun = false)
            assertTrue(sec in 25..35)
        }
    }

    @Test
    fun nextDurationSeconds_shortRunRange() {
        val generator = SegmentGenerator(Random(42))
        repeat(200) {
            val sec = generator.nextDurationSeconds(shortRun = true)
            assertTrue(sec in 10..15)
        }
    }

    @Test
    fun generate_distanceMatchesStrideRoundedToTwoDecimals() {
        val generator = SegmentGenerator(Random(42))
        val level = IntensityLevel.JOG
        val segment = generator.generate(index = 0, startMillis = 1_000L, level = level)
        val expected = round(segment.steps * level.strideMeters * 100.0) / 100.0
        assertEquals(expected.toFloat(), segment.distanceMeters, 0.001f)
    }

    @Test
    fun lastSegmentOverrunConstant_matchesWorstCaseMath() {
        // Prior segment ends at sessionEnd − 1s; max default draw is 35s → +34s.
        assertEquals(
            SegmentGenerator.MAX_DURATION_SEC_EXCLUSIVE - 1 - 1,
            SegmentGenerator.LAST_SEGMENT_OVERRUN_SEC,
        )
    }
}
