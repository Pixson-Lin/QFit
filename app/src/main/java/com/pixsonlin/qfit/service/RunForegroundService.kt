package com.pixsonlin.qfit.service

import android.app.AlarmManager
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
import com.pixsonlin.qfit.data.HistoryRepository.Companion.toSegmentData
import com.pixsonlin.qfit.data.IntensityLevel
import com.pixsonlin.qfit.data.db.RunEntity
import com.pixsonlin.qfit.data.db.RunStatus
import com.pixsonlin.qfit.domain.CatchUpThrottle
import com.pixsonlin.qfit.domain.HealthConnectWriter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.min

class RunForegroundService : LifecycleService() {
    private val repo by lazy { HistoryRepository(this) }
    private val tickMutex = Mutex()

    private var runJob: Job? = null
    private var stopRequested = false
    private var finalized = false
    private var activeRun: RunEntity? = null
    private var wakeLock: RunWakeLock? = null
    private var scheduler: SessionScheduler? = null
    private var screenOnReceiver: ScreenOnReceiver? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> {
                val intensityName = intent.getStringExtra(EXTRA_INTENSITY) ?: return START_NOT_STICKY
                val durationMinutes = intent.getIntExtra(EXTRA_DURATION_MIN, 20)
                val backgroundRun = intent.getBooleanExtra(EXTRA_BACKGROUND_RUN, true)
                val intensity = runCatching { IntensityLevel.valueOf(intensityName) }.getOrNull()
                    ?: return START_NOT_STICKY
                lifecycleScope.launch {
                    beginNewRun(intensity, durationMinutes, backgroundRun)
                }
            }
            ACTION_RESUME -> {
                // May be started via startForegroundService after process death.
                ensureChannel()
                promoteForeground(0, 20)
                val runId = intent.getStringExtra(EXTRA_RUN_ID)
                lifecycleScope.launch { resumeExisting(runId) }
            }
            ACTION_SCHEDULE_TICK -> {
                ensureChannel()
                promoteForeground(0, 20)
                val runId = intent.getStringExtra(EXTRA_RUN_ID)
                lifecycleScope.launch { onScheduleTick(runId) }
            }
            ACTION_CANCEL -> cancelRun()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        ScreenOnReceiver.unregister(this, screenOnReceiver)
        screenOnReceiver = null
        scheduler?.cancel()
        wakeLock?.releaseAll()
        super.onDestroy()
    }

    private suspend fun beginNewRun(
        intensity: IntensityLevel,
        durationMinutes: Int,
        backgroundRun: Boolean,
    ) {
        if (activeRun != null && !finalized) return
        // Satisfy FGS contract before Room planning work.
        if (backgroundRun) {
            ensureChannel()
            promoteForeground(0, durationMinutes)
        }
        val run = repo.startPlannedRun(intensity, durationMinutes, backgroundRun)
        attachAndLoop(run)
    }

    private suspend fun resumeExisting(runId: String?) {
        if (activeRun != null && !finalized) {
            tickMutex.withLock { catchUpDue() }
            return
        }
        val run = when {
            runId != null -> repo.getRunById(runId)
            else -> repo.getRunningRun()
        } ?: return
        if (run.status != RunStatus.RUNNING.name) return
        attachAndLoop(run)
    }

    private suspend fun onScheduleTick(runId: String?) {
        if (activeRun == null || finalized) {
            resumeExisting(runId)
            return
        }
        tickMutex.withLock {
            catchUpDue()
            maybeReschedule()
        }
    }

    private suspend fun attachAndLoop(run: RunEntity) {
        if (runJob?.isActive == true && activeRun?.id == run.id) return
        stopRequested = false
        finalized = false
        activeRun = run
        val intensity = runCatching { IntensityLevel.valueOf(run.intensityName) }.getOrNull()
            ?: return

        RunSessionState.setActive(
            ActiveRunUi(
                runId = run.id,
                intensity = intensity,
                durationMinutes = run.plannedDurationMinutes,
                startTimeMillis = run.startTimeMillis,
                endTimeMillis = run.plannedEndTimeMillis,
                stepsWritten = repo.sumWrittenSteps(run.id),
            ),
        )

        val lock = RunWakeLock(this).also { wakeLock = it }
        if (!canScheduleExactAlarms()) {
            lock.acquireSession()
        }
        scheduler = SessionScheduler(
            context = this,
            runId = run.id,
            wakeLock = lock,
            useForegroundService = run.backgroundRun,
        )

        if (run.backgroundRun) {
            ensureChannel()
            promoteForeground(repo.sumWrittenSteps(run.id), run.plannedDurationMinutes)
        }

        ScreenOnReceiver.unregister(this, screenOnReceiver)
        screenOnReceiver = ScreenOnReceiver.register(this, lifecycleScope) {
            tickMutex.withLock {
                if (!finalized && activeRun != null) {
                    catchUpDue()
                    maybeReschedule()
                }
            }
        }

        runJob?.cancel()
        runJob = lifecycleScope.launch {
            try {
                sessionLoop()
            } catch (_: CancellationException) {
                // cancel / stopSelf
            } finally {
                tickMutex.withLock {
                    finalizeIfNeeded()
                }
                cleanupSession()
                stopSelf()
            }
        }
    }

    private suspend fun sessionLoop() {
        val run = activeRun ?: return
        if (System.currentTimeMillis() >= run.plannedEndTimeMillis || stopRequested) {
            return
        }
        while (!stopRequested && System.currentTimeMillis() < run.plannedEndTimeMillis) {
            tickMutex.withLock {
                catchUpDue()
            }
            if (stopRequested || finalized) break
            if (repo.countPlannedSegments(run.id) == 0) break

            val now = System.currentTimeMillis()
            val next = repo.computeNextBatchDeadlineMillis(run.id, run.batchSize, now)
                ?: run.plannedEndTimeMillis
            val deadline = min(next, run.plannedEndTimeMillis)
            if (deadline > now) {
                scheduler?.scheduleNext(deadline)
                awaitUntil(deadline)
            }
        }
    }

    private suspend fun catchUpDue() {
        val run = activeRun ?: return
        if (finalized) return
        val writeDeadline = min(System.currentTimeMillis(), run.plannedEndTimeMillis)

        while (!stopRequested) {
            val roundStart = System.currentTimeMillis()
            var wroteInRound = false
            var segmentsInRound = 0
            var batchesLeft = CatchUpThrottle.MAX_BATCHES_PER_CATCH_UP

            while (
                batchesLeft > 0 &&
                segmentsInRound < CatchUpThrottle.MAX_SEGMENTS_PER_CATCH_UP &&
                !stopRequested
            ) {
                val effectiveNow = min(System.currentTimeMillis(), writeDeadline)
                val batch = repo.getDuePlannedSegments(
                    runId = run.id,
                    now = effectiveNow,
                    limit = run.batchSize,
                )
                if (batch.isEmpty()) break

                val lock = wakeLock
                lock?.acquireForWrite()
                val writeResult = try {
                    runCatching {
                        HealthConnectWriter.writeSegments(
                            this@RunForegroundService,
                            batch.map { it.toSegmentData() },
                        )
                    }
                } finally {
                    lock?.releaseWrite()
                }

                if (writeResult.isSuccess) {
                    repo.markSegmentsWritten(batch)
                } else {
                    repo.markSegmentsFailed(batch, writeResult.exceptionOrNull()?.message)
                }

                wroteInRound = true
                segmentsInRound += batch.size
                batchesLeft -= 1

                val totalSteps = repo.sumWrittenSteps(run.id)
                RunSessionState.update { it.copy(stepsWritten = totalSteps) }
                if (run.backgroundRun) {
                    notifyProgress(totalSteps, run.plannedDurationMinutes)
                }

                if (writeResult.isFailure) {
                    RunSessionState.update {
                        it.copy(
                            errorMessage = writeResult.exceptionOrNull()?.message
                                ?: writeResult.exceptionOrNull()?.javaClass?.simpleName,
                        )
                    }
                    // Keep going on other due batches; do not abort whole session on one failure.
                }
                if (batchesLeft > 0) {
                    delay(CatchUpThrottle.DELAY_BETWEEN_BATCHES_MS)
                }
            }

            if (!wroteInRound) break
            val stillDue = repo.hasDuePlanned(
                run.id,
                min(System.currentTimeMillis(), writeDeadline),
            )
            if (
                System.currentTimeMillis() - roundStart < CatchUpThrottle.MAX_CATCH_UP_WALL_CLOCK_MS &&
                !stillDue
            ) {
                break
            }
        }
    }

    private suspend fun maybeReschedule() {
        val run = activeRun ?: return
        if (finalized || stopRequested) return
        if (System.currentTimeMillis() >= run.plannedEndTimeMillis) return
        val now = System.currentTimeMillis()
        val next = repo.computeNextBatchDeadlineMillis(run.id, run.batchSize, now)
            ?: return
        if (next > now) {
            scheduler?.scheduleNext(min(next, run.plannedEndTimeMillis))
        }
    }

    private suspend fun finalizeIfNeeded() {
        val run = activeRun ?: return
        if (finalized) return
        val writeDeadline = if (stopRequested) {
            System.currentTimeMillis()
        } else {
            run.plannedEndTimeMillis
        }
        // Temporarily allow catch-up past the loop writeDeadline for natural end.
        val saved = activeRun
        if (saved != null) {
            val dueNow = min(System.currentTimeMillis(), writeDeadline)
            while (true) {
                val batch = repo.getDuePlannedSegments(saved.id, dueNow, saved.batchSize)
                if (batch.isEmpty()) break
                val lock = wakeLock
                lock?.acquireForWrite()
                val writeResult = try {
                    runCatching {
                        HealthConnectWriter.writeSegments(
                            this@RunForegroundService,
                            batch.map { it.toSegmentData() },
                        )
                    }
                } finally {
                    lock?.releaseWrite()
                }
                if (writeResult.isSuccess) {
                    repo.markSegmentsWritten(batch)
                } else {
                    repo.markSegmentsFailed(batch, writeResult.exceptionOrNull()?.message)
                }
                val totalSteps = repo.sumWrittenSteps(saved.id)
                RunSessionState.update { it.copy(stepsWritten = totalSteps) }
            }
        }
        repo.markAllPlannedSkipped(run.id)
        val totalSteps = repo.sumWrittenSteps(run.id)
        val status = if (stopRequested) RunStatus.CANCELLED else RunStatus.COMPLETED
        repo.finalizeRun(run.id, status, totalSteps)
        finalized = true
        RunSessionState.update {
            it.copy(stepsWritten = totalSteps, finished = true)
        }
        if (run.backgroundRun) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

    private fun cleanupSession() {
        ScreenOnReceiver.unregister(this, screenOnReceiver)
        screenOnReceiver = null
        scheduler?.cancel()
        scheduler = null
        wakeLock?.releaseAll()
        wakeLock = null
        activeRun = null
        runJob = null
    }

    private fun cancelRun() {
        stopRequested = true
        runJob?.cancel()
        if (runJob == null || runJob?.isCompleted == true) {
            lifecycleScope.launch {
                tickMutex.withLock { finalizeIfNeeded() }
                cleanupSession()
                stopSelf()
            }
        }
    }

    private suspend fun awaitUntil(deadlineMillis: Long) {
        while (!stopRequested && System.currentTimeMillis() < deadlineMillis) {
            val remaining = deadlineMillis - System.currentTimeMillis()
            delay(min(remaining, CatchUpThrottle.AWAIT_CHUNK_MS))
        }
    }

    private fun canScheduleExactAlarms(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
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

    private fun promoteForeground(steps: Int, durationMinutes: Int) {
        val notification = buildNotification(getString(R.string.notif_running), steps, durationMinutes)
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
    }

    private fun notifyProgress(steps: Int, durationMinutes: Int) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(
            NOTIFICATION_ID,
            buildNotification(getString(R.string.notif_running), steps, durationMinutes),
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
        const val ACTION_RESUME = "com.pixsonlin.qfit.action.RESUME_RUN"
        const val ACTION_CANCEL = "com.pixsonlin.qfit.action.CANCEL_RUN"
        const val ACTION_SCHEDULE_TICK = "com.pixsonlin.qfit.action.SCHEDULE_TICK"
        const val EXTRA_INTENSITY = "intensity"
        const val EXTRA_DURATION_MIN = "duration_min"
        const val EXTRA_BACKGROUND_RUN = "background_run"
        const val EXTRA_RUN_ID = "run_id"
        private const val CHANNEL_ID = "qfit_run"
        private const val NOTIFICATION_ID = 42

        fun start(
            context: Context,
            intensity: IntensityLevel,
            durationMinutes: Int,
            backgroundRun: Boolean,
        ) {
            val intent = Intent(context, RunForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_INTENSITY, intensity.name)
                putExtra(EXTRA_DURATION_MIN, durationMinutes)
                putExtra(EXTRA_BACKGROUND_RUN, backgroundRun)
            }
            if (backgroundRun) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        }

        fun resume(context: Context, run: RunEntity) {
            val intent = Intent(context, RunForegroundService::class.java).apply {
                action = ACTION_RESUME
                putExtra(EXTRA_RUN_ID, run.id)
            }
            if (run.backgroundRun) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        }

        fun cancel(context: Context) {
            val intent = Intent(context, RunForegroundService::class.java).apply {
                action = ACTION_CANCEL
            }
            context.startService(intent)
        }
    }
}
