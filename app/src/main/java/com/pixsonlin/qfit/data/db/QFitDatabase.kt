package com.pixsonlin.qfit.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [RunEntity::class, SegmentEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class QFitDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var instance: QFitDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE runs ADD COLUMN plannedEndTimeMillis INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "ALTER TABLE runs ADD COLUMN batchSize INTEGER NOT NULL DEFAULT 3",
                )
                db.execSQL(
                    "ALTER TABLE runs ADD COLUMN backgroundRun INTEGER NOT NULL DEFAULT 1",
                )
                db.execSQL(
                    "UPDATE runs SET plannedEndTimeMillis = " +
                        "startTimeMillis + plannedDurationMinutes * 60000 " +
                        "WHERE plannedEndTimeMillis = 0",
                )
                db.execSQL(
                    "ALTER TABLE segments ADD COLUMN writeStatus TEXT NOT NULL DEFAULT 'WRITTEN'",
                )
                db.execSQL(
                    "UPDATE segments SET writeStatus = " +
                        "CASE WHEN success = 1 THEN 'WRITTEN' ELSE 'FAILED' END",
                )
            }
        }

        /**
         * Session HC write redesign: one Distance+Exercise at finalize.
         * Mark existing RUNNING rows as already session-written so an upgrade mid-run
         * does not add a second activity on top of old per-segment Exercise rows.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE runs ADD COLUMN simulateActivityDistance " +
                        "INTEGER NOT NULL DEFAULT 1",
                )
                db.execSQL(
                    "ALTER TABLE runs ADD COLUMN sessionHcWritten INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "UPDATE runs SET sessionHcWritten = 1 WHERE status = 'RUNNING'",
                )
            }
        }

        fun get(context: Context): QFitDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    QFitDatabase::class.java,
                    "qfit.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
    }
}
