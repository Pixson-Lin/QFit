package com.pixsonlin.qfit.domain

object CatchUpThrottle {
    const val MAX_BATCHES_PER_CATCH_UP = 3
    const val DELAY_BETWEEN_BATCHES_MS = 1_000L
    const val MAX_SEGMENTS_PER_CATCH_UP = 20
    const val MAX_CATCH_UP_WALL_CLOCK_MS = 30_000L
    const val AWAIT_CHUNK_MS = 5_000L
}
