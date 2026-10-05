package com.pixsonlin.qfit.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "runs")
data class RunEntity(
    @PrimaryKey val id: String,
    val intensityName: String,
    val intensityDisplayName: String,
    val plannedDurationMinutes: Int,
    val startTimeMillis: Long,
    /** Wall-clock planned end (= start + duration). */
    val plannedEndTimeMillis: Long,
    /** Actual end when finished; equals start while RUNNING. */
    val endTimeMillis: Long,
    val totalSteps: Int,
    val status: String,
    val batchSize: Int = DEFAULT_BATCH_SIZE,
    val backgroundRun: Boolean = true,
) {
    companion object {
        const val DEFAULT_BATCH_SIZE = 2
    }
}

@Entity(
    tableName = "segments",
    foreignKeys = [
        ForeignKey(
            entity = RunEntity::class,
            parentColumns = ["id"],
            childColumns = ["runId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("runId")],
)
data class SegmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val runId: String,
    val segmentIndex: Int,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val steps: Int,
    val distanceMeters: Double,
    val writeStatus: String = SegmentWriteStatus.PLANNED.name,
    val success: Boolean = false,
    val errorMessage: String? = null,
)

data class RunWithSegments(
    @Embedded val run: RunEntity,
    @Relation(parentColumn = "id", entityColumn = "runId")
    val segments: List<SegmentEntity>,
)
