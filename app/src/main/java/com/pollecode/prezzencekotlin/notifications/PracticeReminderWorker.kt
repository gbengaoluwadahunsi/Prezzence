package com.pollecode.prezzencekotlin.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pollecode.prezzencekotlin.R

/**
 * Posts the pre-interview practice nudge. Message copy scales with urgency so a user whose
 * interview is today gets a sharper prompt than one who is just exploring.
 */
class PracticeReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val interviewWhen = inputData.getString(KEY_INTERVIEW_WHEN) ?: "exploring"
        ReminderScheduler.ensureChannel(applicationContext)

        val (title, body) = messageFor(interviewWhen)
        val notification = NotificationCompat.Builder(applicationContext, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.prezzence_icon)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        if (hasNotificationPermission()) {
            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        }
        return Result.success()
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun messageFor(interviewWhen: String): Pair<String, String> = when (interviewWhen) {
        "today" -> "Your interview is today 💪" to
            "Run one last practice round so you walk in sharp and confident."
        "this_week" -> "Your interview is coming up" to
            "A quick practice session now keeps your answers crisp for the big day."
        else -> "Keep your interview skills warm" to
            "Squeeze in a short practice session — a few minutes makes a difference."
    }

    companion object {
        const val KEY_INTERVIEW_WHEN = "interview_when"
        private const val NOTIFICATION_ID = 4201
    }
}
