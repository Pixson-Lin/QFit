package com.pixsonlin.qfit.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(run: RunEntity)

    @Insert
    suspend fun insertSegments(segments: List<SegmentEntity>)

    @Transaction
    @Query("SELECT * FROM runs ORDER BY startTimeMillis DESC")
    fun observeRunsWithSegments(): Flow<List<RunWithSegments>>

    @Query("DELETE FROM runs")
    suspend fun clearAll()
}
