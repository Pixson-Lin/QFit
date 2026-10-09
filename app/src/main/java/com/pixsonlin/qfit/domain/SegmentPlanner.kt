package com.pixsonlin.qfit.domain

import com.pixsonlin.qfit.data.IntensityLevel

class SegmentPlanner {
    fun planAllSegments(
        runStartMillis: Long,
        sessionEndMillis: Long,
        intensity: IntensityLevel,
        generator: SegmentGenerator = SegmentGenerator(),
    ): List<SegmentData> {
        val sessionDurationMs = (sessionEndMillis - runStartMillis).coerceAtLeast(0L)
        val shortRun =
            sessionDurationMs <= SegmentGenerator.SHORT_RUN_THRESHOLD_MINUTES * 60_000L
        // Allow last segment to finish slightly after configured end (see LAST_SEGMENT_OVERRUN_SEC).
        val coverageEndMillis =
            sessionEndMillis + SegmentGenerator.LAST_SEGMENT_OVERRUN_SEC * 1_000L

        val segments = mutableListOf<SegmentData>()
        var nextStart = runStartMillis
        var index = 0
        while (nextStart < sessionEndMillis) {
            val durationSec = generator.nextDurationSeconds(shortRun = shortRun)
            val endMillis = nextStart + durationSec * 1_000L
            if (endMillis > coverageEndMillis) break
            val segment = generator.generate(
                index = index,
                startMillis = nextStart,
                level = intensity,
                durationSec = durationSec,
            )
            segments += segment
            index += 1
            nextStart = segment.endTimeMillis
        }
        return segments
    }
}
