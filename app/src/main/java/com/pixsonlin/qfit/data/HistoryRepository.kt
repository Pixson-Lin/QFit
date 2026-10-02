package com.pixsonlin.qfit.data

import android.content.Context
import com.pixsonlin.qfit.data.db.QFitDatabase
import com.pixsonlin.qfit.data.db.RunEntity
import com.pixsonlin.qfit.data.db.RunWithSegments
import com.pixsonlin.qfit.data.db.SegmentEntity
import kotlinx.coroutines.flow.Flow

class HistoryRepository(context: Context) {
    private val dao = QFitDatabase.get(context).historyDao()

    fun observeHistory(): Flow<List<RunWithSegments>> = dao.observeRunsWithSegments()

    suspend fun saveRun(run: RunEntity, segments: List<SegmentEntity>) {
        dao.insertRun(run)
        if (segments.isNotEmpty()) {
            dao.insertSegments(segments)
        }
    }

    suspend fun clearAll() {
        dao.clearAll()
    }
}
