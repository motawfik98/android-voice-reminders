package com.motawfik.voicereminders

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.JobIntentService
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationIntentService: JobIntentService() {
    private val CHANNEL_ID = "ONLY_CHANNEL"

    override fun onHandleWork(intent: Intent) {
        // Create an explicit intent for an Activity in your app
        val notifyIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, notifyIntent, 0)


        val title = intent.getStringExtra("EVENT_TITLE")
        val notificationID = intent.getIntExtra("NOTIFICATION_ID", 0)
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.mic)
            .setContentTitle(title)
            .setContentText("This notification is to remind you before the main event by 24 hours")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            // notificationId is a unique int for each notification that you must define
            notify(notificationID, builder.build())
        }

    }

}