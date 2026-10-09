package com.pixsonlin.qfit.domain

import com.pixsonlin.qfit.data.IntensityLevel
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SegmentPlannerTest {
    private val planner = SegmentPlanner()

    @Test
    fun planAllSegments_allowsLastSegmentOverrunUpTo34s() {
        val start = 1_000_000L
        val sessionEnd = start + 60_000L // 1 minute
        val coverageEnd = sessionEnd + SegmentGenerator.LAST_SEGMENT_OVERRUN_SEC * 1_000L
        repeat(40) { seed ->
            val segments = planner.planAllSegments(
                runStartMillis = start,
                sessionEndMillis = sessionEnd,
                intensity = IntensityLevel.JOG,
                generator = SegmentGenerator(Random(seed)),
            )
            assertTrue("seed=$seed expected non-empty plan", segments.isNotEmpty())
            segments.forEach { segment ->
                assertTrue(segment.startTimeMillis >= start)
                assertTrue(
                    "seed=$seed end=${segment.endTimeMillis} coverageEnd=$coverageEnd",
                    segment.endTimeMillis <= coverageEnd,
                )
            }
            // With +34s overrun, 1-minute runs should cover close to the configured end
            // (not stop ~30s early from a hard sessionEnd cut).
            val lastEnd = segments.last().endTimeMillis
            assertTrue(
                "seed=$seed lastEnd=$lastEnd should reach near sessionEnd=$sessionEnd",
                lastEnd >= sessionEnd - 15_000L,
            )
        }
    }

    @Test
    fun planAllSegments_neverStartsPastConfiguredEnd() {
        val start = 0L
        val sessionEnd = 3 * 60_000L
        val segments = planner.planAllSegments(
            runStartMillis = start,
            sessionEndMillis = sessionEnd,
            intensity = IntensityLevel.STROLL,
            generator = SegmentGenerator(Random(11)),
        )
        segments.forEach { segment ->
            assertTrue(segment.startTimeMillis < sessionEnd)
        }
    }

    @Test
    fun planAllSegments_shortRun_usesShorterSegments() {
        val start = 0L
        val sessionEnd = 3 * 60_000L // ≤5 min → short-run mode
        repeat(20) { seed ->
            val segments = planner.planAllSegments(
                runStartMillis = start,
                sessionEndMillis = sessionEnd,
                intensity = IntensityLevel.SUPER_SLOW_JOG,
                generator = SegmentGenerator(Random(seed.toLong())),
            )
            assertTrue(segments.isNotEmpty())
            segments.forEach { segment ->
                val durationSec = (segment.endTimeMillis - segment.startTimeMillis) / 1_000L
                assertTrue(
                    "seed=$seed durationSec=$durationSec",
                    durationSec in
                        SegmentGenerator.SHORT_MIN_DURATION_SEC.toLong() until
                        SegmentGenerator.SHORT_MAX_DURATION_SEC_EXCLUSIVE.toLong(),
                )
            }
        }
    }

    @Test
    fun planAllSegments_longRun_usesDefaultSegmentLength() {
        val start = 0L
        val sessionEnd = 10 * 60_000L // >5 min
        val segments = planner.planAllSegments(
            runStartMillis = start,
            sessionEndMillis = sessionEnd,
            intensity = IntensityLevel.JOG,
            generator = SegmentGenerator(Random(7)),
        )
        assertTrue(segments.size in 17..25)
        segments.forEach { segment ->
            val durationSec = (segment.endTimeMillis - segment.startTimeMillis) / 1_000L
            assertTrue(durationSec in 25L..35L)
        }
    }
}
