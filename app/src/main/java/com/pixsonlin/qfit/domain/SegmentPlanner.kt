package com.pixsonlin.qfit.domain

import com.pixsonlin.qfit.data.IntensityLevel

class SegmentPlanner {
    fun planAllSegments(
        runStartMillis: Long,
        sessionEndMillis: Long,
        intensity: IntensityLevel,
        generator: SegmentGenerator = SegmentGenerator(),
    ): List<SegmentData> {
        val segments = mutableListOf<SegmentData>()
        var nextStart = runStartMillis
        var index = 0
        while (nextStart < sessionEndMillis) {
            val durationSec = generator.nextDurationSeconds()
            val endMillis = nextStart + durationSec * 1_000L
            if (endMillis > sessionEndMillis) break
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
