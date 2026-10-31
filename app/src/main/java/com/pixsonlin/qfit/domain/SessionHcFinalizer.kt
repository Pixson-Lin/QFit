package com.pixsonlin.qfit.domain

import android.content.Context
import com.pixsonlin.qfit.data.HistoryRepository
import com.pixsonlin.qfit.data.IntensityLevel
import com.pixsonlin.qfit.data.db.RunEntity

/**
 * Writes one Distance + one Exercise for a run's WRITTEN span (or marks done if skipped).
 * Idempotent via [RunEntity.sessionHcWritten].
 */
object SessionHcFinalizer {
    /**
     * @return true if session HC work is complete (written or intentionally skipped);
     * false if a write was attempted and failed (caller may retry later).
     */
    suspend fun ensureSessionWritten(
        context: Context,
        repo: HistoryRepository,
        run: RunEntity,
    ): Boolean {
        val fresh = repo.getRunById(run.id) ?: return true
        if (fresh.sessionHcWritten) return true

        if (!fresh.simulateActivityDistance) {
            repo.markSessionHcWritten(fresh.id)
            return true
        }

        val span = repo.writtenSpan(fresh.id)
        if (span == null) {
            repo.markSessionHcWritten(fresh.id)
            return true
        }

        val intensity = runCatching { IntensityLevel.valueOf(fresh.intensityName) }.getOrNull()
            ?: IntensityLevel.SUPER_SLOW_JOG
        val writeResult = runCatching {
            HealthConnectWriter.writeSessionSummary(
                context = context,
                startTimeMillis = span.startTimeMillis,
                endTimeMillis = span.endTimeMillis,
                distanceMeters = span.distanceMeters,
                exerciseType = HealthConnectWriter.exerciseTypeFor(intensity),
            )
        }
        if (writeResult.isSuccess) {
            repo.markSessionHcWritten(fresh.id)
            return true
        }
        return false
    }

    /** Retry finished runs that still need a session HC write (e.g. prior insert failure). */
    suspend fun retryPending(context: Context, repo: HistoryRepository) {
        for (run in repo.getRunsPendingSessionHc()) {
            ensureSessionWritten(context, repo, run)
        }
    }
}
