package com.pollecode.prezzencekotlin.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedules a single pre-interview practice nudge whose timing is derived from how soon the
 * user's interview is (`interviewWhen`). This is what makes the "Practice reminders" toggle real:
 * the closer the interview, the sooner we nudge them back in to practice.
 */
object ReminderScheduler {
    const val CHANNEL_ID = "practice_reminders"
    private const val WORK_NAME = "practice_reminder"

    fun schedule(context: Context, interviewWhen: String) {
        ensureChannel(context)
        val delayMinutes = when (interviewWhen) {
            "today" -> 120L                 // ~2 hours: one more rep before today's interview
            "this_week" -> 20L * 60L        // next day
            "this_month" -> 3L * 24L * 60L  // in a few days
            else -> 3L * 24L * 60L          // "exploring": a gentle nudge
        }
        val request = OneTimeWorkRequestBuilder<PracticeReminderWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setInputData(
                Data.Builder()
                    .putString(PracticeReminderWorker.KEY_INTERVIEW_WHEN, interviewWhen)
                    .build()
            )
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Practice reminders",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = "Nudges to keep your interview prep on track" }
        manager.createNotificationChannel(channel)
    }
}
