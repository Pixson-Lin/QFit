package com.pixsonlin.qfit.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(run: RunEntity)

    @Update
    suspend fun updateRun(run: RunEntity)

    @Insert
    suspend fun insertSegments(segments: List<SegmentEntity>)

    @Update
    suspend fun updateSegments(segments: List<SegmentEntity>)

    @Query("SELECT * FROM runs WHERE id = :runId LIMIT 1")
    suspend fun getRunById(runId: String): RunEntity?

    @Query("SELECT * FROM runs WHERE status = 'RUNNING' ORDER BY startTimeMillis DESC LIMIT 1")
    suspend fun getRunningRun(): RunEntity?

    @Transaction
    @Query(
        "SELECT * FROM runs WHERE status != 'RUNNING' ORDER BY startTimeMillis DESC",
    )
    fun observeFinishedRunsWithSegments(): Flow<List<RunWithSegments>>

    @Query(
        "SELECT * FROM segments WHERE runId = :runId AND writeStatus = 'PLANNED' " +
            "AND endTimeMillis <= :now ORDER BY segmentIndex ASC LIMIT :limit",
    )
    suspend fun getDuePlanned(runId: String, now: Long, limit: Int): List<SegmentEntity>

    @Query(
        "SELECT * FROM segments WHERE runId = :runId AND writeStatus = 'PLANNED' " +
            "ORDER BY segmentIndex ASC LIMIT :limit",
    )
    suspend fun getNextPlannedBatch(runId: String, limit: Int): List<SegmentEntity>

    @Query(
        "SELECT COUNT(*) FROM segments WHERE runId = :runId AND writeStatus = 'PLANNED'",
    )
    suspend fun countPlannedSegments(runId: String): Int

    @Query(
        "SELECT COALESCE(SUM(steps), 0) FROM segments " +
            "WHERE runId = :runId AND writeStatus = 'WRITTEN'",
    )
    suspend fun sumWrittenSteps(runId: String): Int

    @Query(
        "UPDATE segments SET writeStatus = 'SKIPPED', success = 0 " +
            "WHERE runId = :runId AND writeStatus = 'PLANNED'",
    )
    suspend fun markAllPlannedSkipped(runId: String)

    @Query("DELETE FROM runs")
    suspend fun clearAll()
}
