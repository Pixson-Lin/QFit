package com.pixsonlin.qfit.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.pixsonlin.qfit.MainActivity
import com.pixsonlin.qfit.R
import com.pixsonlin.qfit.data.HistoryRepository
import com.pixsonlin.qfit.data.IntensityLevel
import com.pixsonlin.qfit.data.db.RunEntity
import com.pixsonlin.qfit.data.db.RunStatus
import com.pixsonlin.qfit.data.db.SegmentEntity
import com.pixsonlin.qfit.domain.HealthConnectWriter
import com.pixsonlin.qfit.domain.SegmentGenerator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class RunForegroundService : LifecycleService() {
    private var runJob: Job? = null
    private val generator = SegmentGenerator()
    private var userCancelled = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> {
                val intensityName = intent.getStringExtra(EXTRA_INTENSITY) ?: return START_NOT_STICKY
                val durationMinutes = intent.getIntExtra(EXTRA_DURATION_MIN, 20)
                val intensity = runCatching { IntensityLevel.valueOf(intensityName) }.getOrNull()
                    ?: return START_NOT_STICKY
                startRun(intensity, durationMinutes)
            }
            ACTION_CANCEL -> cancelRun()
        }
        return START_STICKY
    }

    private fun startRun(intensity: IntensityLevel, durationMinutes: Int) {
        runJob?.cancel()
        userCancelled = false
        val runId = UUID.randomUUID().toString()
        val start = System.currentTimeMillis()
        val plannedEnd = start + durationMinutes * 60_000L
        RunSessionState.setActive(
            ActiveRunUi(
                runId = runId,
                intensity = intensity,
                durationMinutes = durationMinutes,
                startTimeMillis = start,
                endTimeMillis = plannedEnd,
            ),
        )
        ensureChannel()
        val notification = buildNotification(getString(R.string.notif_running), 0, durationMinutes)
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            @Suppress("DEPRECATION")
            startForeground(NOTIFICATION_ID, notification)
        }

        runJob = lifecycleScope.launch {
            val pending = mutableListOf<SegmentEntity>()
            var cursor = start
            var index = 0
            var totalSteps = 0
            var status = RunStatus.COMPLETED
            try {
                while (isActive && cursor < plannedEnd) {
                    val remainingSec = ((plannedEnd - cursor) / 1000L).toInt().coerceAtLeast(1)
                    val durationSec = generator.nextDurationSeconds().coerceAtMost(remainingSec)
                    val segment = generator.generate(
                        index = index,
                        startMillis = cursor,
                        level = intensity,
                        durationSec = durationSec,
                    )
                    val waitMs = (segment.endTimeMillis - System.currentTimeMillis()).coerceAtLeast(0L)
                    delay(waitMs)
                    if (!isActive) break
                    if (segment.endTimeMillis > System.currentTimeMillis()) {
                        delay(segment.endTimeMillis - System.currentTimeMillis())
                    }
                    val writeResult = runCatching {
                        HealthConnectWriter.writeSegments(
                            this@RunForegroundService,
                            listOf(segment),
                        )
                    }
                    pending += SegmentEntity(
                        runId = runId,
                        segmentIndex = index,
                        startTimeMillis = segment.startTimeMillis,
                        endTimeMillis = segment.endTimeMillis,
                        steps = segment.steps,
                        distanceMeters = segment.distanceMeters.toDouble(),
                        success = writeResult.isSuccess,
                        errorMessage = writeResult.exceptionOrNull()?.message,
                    )
                    if (writeResult.isSuccess) {
                        totalSteps += segment.steps
                    }
                    index += 1
                    cursor = segment.endTimeMillis
                    RunSessionState.update { it.copy(stepsWritten = totalSteps) }
                    val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(
                        NOTIFICATION_ID,
                        buildNotification(
                            getString(R.string.notif_running),
                            totalSteps,
                            durationMinutes,
                        ),
                    )
                }
                if (userCancelled) status = RunStatus.CANCELLED
            } catch (ce: CancellationException) {
                status = RunStatus.CANCELLED
                throw ce
            } catch (t: Throwable) {
                status = RunStatus.CANCELLED
                RunSessionState.update {
                    it.copy(errorMessage = t.message ?: t::class.java.simpleName)
                }
            } finally {
                if (userCancelled) status = RunStatus.CANCELLED
                val endMillis = System.currentTimeMillis()
                runCatching {
                    HistoryRepository(this@RunForegroundService).saveRun(
                        run = RunEntity(
                            id = runId,
                            intensityName = intensity.name,
                            intensityDisplayName = intensity.displayName,
                            plannedDurationMinutes = durationMinutes,
                            startTimeMillis = start,
                            endTimeMillis = endMillis,
                            totalSteps = totalSteps,
                            status = status.name,
                        ),
                        segments = pending.sortedBy { it.segmentIndex },
                    )
                }
                RunSessionState.update { it.copy(finished = true) }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun cancelRun() {
        userCancelled = true
        runJob?.cancel()
        runJob = null
    }

    private fun ensureChannel() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    private fun buildNotification(title: String, steps: Int, durationMinutes: Int): Notification {
        val launch = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(getString(R.string.notif_body, steps, durationMinutes))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(launch)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val ACTION_START = "com.pixsonlin.qfit.action.START_RUN"
        const val ACTION_CANCEL = "com.pixsonlin.qfit.action.CANCEL_RUN"
        const val EXTRA_INTENSITY = "intensity"
        const val EXTRA_DURATION_MIN = "duration_min"
        private const val CHANNEL_ID = "qfit_run"
        private const val NOTIFICATION_ID = 42

        fun start(context: Context, intensity: IntensityLevel, durationMinutes: Int) {
            val intent = Intent(context, RunForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_INTENSITY, intensity.name)
                putExtra(EXTRA_DURATION_MIN, durationMinutes)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun cancel(context: Context) {
            val intent = Intent(context, RunForegroundService::class.java).apply {
                action = ACTION_CANCEL
            }
            context.startService(intent)
        }
    }
}
