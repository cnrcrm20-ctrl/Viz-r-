package com.example.util

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.RemoteInput

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        when (intent.action) {
            NotificationManagerHelper.ACTION_ACCEPT_CALL -> {
                Toast.makeText(context, "Arama kabul edildi / Call Accepted", Toast.LENGTH_SHORT).show()
                notificationManager.cancel(1) // Assuming call ID is 1
            }
            NotificationManagerHelper.ACTION_DECLINE_CALL -> {
                Toast.makeText(context, "Arama reddedildi / Call Declined", Toast.LENGTH_SHORT).show()
                notificationManager.cancel(1)
            }
            NotificationManagerHelper.ACTION_REPLY_MESSAGE -> {
                val remoteInput = RemoteInput.getResultsFromIntent(intent)
                val replyText = remoteInput?.getCharSequence("key_text_reply")?.toString()
                val sender = intent.getStringExtra("sender") ?: "Unknown"

                if (replyText != null) {
                    // Send to Firebase or local simulation here
                    Toast.makeText(context, "Mesaj gönderildi: \$replyText", Toast.LENGTH_SHORT).show()
                    
                    // Dismiss the notification
                    notificationManager.cancel(sender.hashCode())
                }
            }
        }
    }
}
