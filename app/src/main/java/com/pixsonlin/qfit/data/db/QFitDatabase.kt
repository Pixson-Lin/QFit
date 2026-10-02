package com.pixsonlin.qfit.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [RunEntity::class, SegmentEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class QFitDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var instance: QFitDatabase? = null

        fun get(context: Context): QFitDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    QFitDatabase::class.java,
                    "qfit.db",
                ).build().also { instance = it }
            }
    }
}
