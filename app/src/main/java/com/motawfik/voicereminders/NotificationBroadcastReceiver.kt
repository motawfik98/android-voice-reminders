package com.motawfik.voicereminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService

class NotificationBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {
        val title = p1?.getStringExtra("EVENT_TITLE")
        val notificationID = p1?.getIntExtra("NOTIFICATION_ID", 0)
        val intent1 = Intent(p0, NotificationIntentService::class.java)
        intent1.putExtra("EVENT_TITLE", title)
        intent1.putExtra("NOTIFICATION_ID", notificationID)
        JobIntentService.enqueueWork(p0!!, NotificationIntentService::class.java, 5,  intent1)
    }
}