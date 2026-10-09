package com.pixsonlin.qfit.data

import android.content.Context
import com.pixsonlin.qfit.data.db.QFitDatabase
import com.pixsonlin.qfit.data.db.RunEntity
import com.pixsonlin.qfit.data.db.RunStatus
import com.pixsonlin.qfit.data.db.RunWithSegments
import com.pixsonlin.qfit.data.db.SegmentEntity
import com.pixsonlin.qfit.data.db.SegmentWriteStatus
import com.pixsonlin.qfit.domain.SegmentData
import com.pixsonlin.qfit.domain.SegmentPlanner
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import kotlin.math.max

class HistoryRepository(context: Context) {
    private val dao = QFitDatabase.get(context).historyDao()
    private val planner = SegmentPlanner()

    fun observeHistory(): Flow<List<RunWithSegments>> = dao.observeFinishedRunsWithSegments()

    suspend fun getRunningRun(): RunEntity? = dao.getRunningRun()

    suspend fun getRunById(runId: String): RunEntity? = dao.getRunById(runId)

    suspend fun startPlannedRun(
        intensity: IntensityLevel,
        durationMinutes: Int,
        backgroundRun: Boolean,
        batchSize: Int = RunEntity.DEFAULT_BATCH_SIZE,
    ): RunEntity {
        val runId = UUID.randomUUID().toString()
        val start = System.currentTimeMillis()
        val plannedEnd = start + durationMinutes * 60_000L
        val run = RunEntity(
            id = runId,
            intensityName = intensity.name,
            // Store enum key; UI translates via intensityName at display time.
            intensityDisplayName = intensity.name,
            plannedDurationMinutes = durationMinutes,
            startTimeMillis = start,
            plannedEndTimeMillis = plannedEnd,
            endTimeMillis = start,
            totalSteps = 0,
            status = RunStatus.RUNNING.name,
            batchSize = batchSize,
            backgroundRun = backgroundRun,
        )
        dao.insertRun(run)
        val planned = planner.planAllSegments(start, plannedEnd, intensity)
        if (planned.isNotEmpty()) {
            dao.insertSegments(
                planned.map { segment ->
                    SegmentEntity(
                        runId = runId,
                        segmentIndex = segment.segmentIndex,
                        startTimeMillis = segment.startTimeMillis,
                        endTimeMillis = segment.endTimeMillis,
                        steps = segment.steps,
                        distanceMeters = segment.distanceMeters.toDouble(),
                        writeStatus = SegmentWriteStatus.PLANNED.name,
                        success = false,
                    )
                },
            )
        }
        return run
    }

    /**
     * Wall-clock end the session must cover: last planned segment end, at least [RunEntity.plannedEndTimeMillis].
     * With last-segment overrun, this can be up to ~+34s past the configured duration.
     */
    suspend fun coverageEndMillis(run: RunEntity): Long {
        val lastSegEnd = dao.maxSegmentEndTimeMillis(run.id) ?: return run.plannedEndTimeMillis
        return max(run.plannedEndTimeMillis, lastSegEnd)
    }

    suspend fun getDuePlannedSegments(runId: String, now: Long, limit: Int): List<SegmentEntity> =
        dao.getDuePlanned(runId, now, limit)

    suspend fun computeNextBatchDeadlineMillis(runId: String, batchSize: Int, now: Long): Long? {
        val batch = dao.getNextPlannedBatch(runId, batchSize)
        if (batch.isEmpty()) return null
        val deadline = batch.maxOf { it.endTimeMillis }
        return if (deadline <= now) now else deadline
    }

    suspend fun hasDuePlanned(runId: String, now: Long): Boolean =
        dao.getDuePlanned(runId, now, 1).isNotEmpty()

    suspend fun countPlannedSegments(runId: String): Int = dao.countPlannedSegments(runId)

    suspend fun sumWrittenSteps(runId: String): Int = dao.sumWrittenSteps(runId)

    suspend fun markSegmentsWritten(batch: List<SegmentEntity>) {
        if (batch.isEmpty()) return
        dao.updateSegments(
            batch.map {
                it.copy(
                    writeStatus = SegmentWriteStatus.WRITTEN.name,
                    success = true,
                    errorMessage = null,
                )
            },
        )
    }

    suspend fun markSegmentsFailed(batch: List<SegmentEntity>, message: String?) {
        if (batch.isEmpty()) return
        dao.updateSegments(
            batch.map {
                it.copy(
                    writeStatus = SegmentWriteStatus.FAILED.name,
                    success = false,
                    errorMessage = message,
                )
            },
        )
    }

    suspend fun markAllPlannedSkipped(runId: String) {
        dao.markAllPlannedSkipped(runId)
    }

    suspend fun finalizeRun(
        runId: String,
        status: RunStatus,
        totalSteps: Int,
    ) {
        val existing = dao.getRunById(runId) ?: return
        dao.updateRun(
            existing.copy(
                status = status.name,
                totalSteps = totalSteps,
                endTimeMillis = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun clearAll() {
        dao.clearAll()
    }

    companion object {
        fun SegmentEntity.toSegmentData(): SegmentData =
            SegmentData(
                segmentIndex = segmentIndex,
                startTimeMillis = startTimeMillis,
                endTimeMillis = endTimeMillis,
                steps = steps,
                distanceMeters = distanceMeters.toFloat(),
            )
    }
}
