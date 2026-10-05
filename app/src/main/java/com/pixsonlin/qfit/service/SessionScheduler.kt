package com.pixsonlin.qfit.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

class SessionScheduler(
    private val context: Context,
    private val runId: String,
    private val wakeLock: RunWakeLock,
    private val useForegroundService: Boolean,
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleNext(triggerAtMillis: Long) {
        val pendingIntent = pendingIntent()
        alarmManager.cancel(pendingIntent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            wakeLock.acquireSession()
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent,
            )
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent,
            )
        }
    }

    fun cancel() {
        alarmManager.cancel(pendingIntent())
    }

    private fun pendingIntent(): PendingIntent {
        val intent = Intent(context, RunForegroundService::class.java).apply {
            action = RunForegroundService.ACTION_SCHEDULE_TICK
            putExtra(RunForegroundService.EXTRA_RUN_ID, runId)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return if (useForegroundService) {
            PendingIntent.getForegroundService(context, runId.hashCode(), intent, flags)
        } else {
            PendingIntent.getService(context, runId.hashCode(), intent, flags)
        }
    }
}
