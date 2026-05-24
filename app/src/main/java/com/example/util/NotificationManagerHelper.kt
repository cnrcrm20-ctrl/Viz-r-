package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.example.MainActivity
import com.example.R

object NotificationManagerHelper {

    private const val CHANNEL_CALLS = "vizor_calls_channel"
    private const val CHANNEL_MESSAGES = "vizor_messages_channel"
    
    const val ACTION_ACCEPT_CALL = "com.example.ACTION_ACCEPT_CALL"
    const val ACTION_DECLINE_CALL = "com.example.ACTION_DECLINE_CALL"
    const val ACTION_REPLY_MESSAGE = "com.example.ACTION_REPLY_MESSAGE"
    
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val configManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Calls Channel (High Priority)
            val callChannel = NotificationChannel(
                CHANNEL_CALLS,
                "Aramalar / Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Gelen aramalar için bildirimler"
                // setSound() could be custom
            }
            
            // Messages Channel (High Priority for Heads-up)
            val msgChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "Mesajlar / Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Gelen direkt mesajlar"
            }
            
            configManager.createNotificationChannel(callChannel)
            configManager.createNotificationChannel(msgChannel)
        }
    }

    fun showCallNotification(context: Context, callerName: String) {
        if (!PermissionsHelper.hasPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)) return

        val acceptIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_ACCEPT_CALL
        }
        val acceptPending = PendingIntent.getBroadcast(
            context, 100, acceptIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val declineIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_DECLINE_CALL
        }
        val declinePending = PendingIntent.getBroadcast(
            context, 101, declineIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_CALLS)
            .setSmallIcon(android.R.drawable.sym_call_incoming) // Placeholder
            .setContentTitle("Gelen Arama / Incoming Call")
            .setContentText("$callerName sizi arıyor...")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(android.R.drawable.ic_menu_call, "Kabul Et / Accept", acceptPending)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Reddet / Decline", declinePending)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, builder.build())
    }

    fun showMessageNotification(context: Context, sender: String, messageText: String) {
        if (!PermissionsHelper.hasPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)) return

        val replyLabel = "Quick Reply"
        val remoteInput = RemoteInput.Builder("key_text_reply")
            .setLabel(replyLabel)
            .build()
            
        val replyIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_REPLY_MESSAGE
            putExtra("sender", sender)
        }
        val replyPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(
                context,
                200,
                replyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

        val action = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send,
            "Yanıtla / Reply",
            replyPendingIntent
        ).addRemoteInput(remoteInput).build()

        val mainIntent = Intent(context, MainActivity::class.java)
        val mainPending = PendingIntent.getActivity(context, 201, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle("Yeni Mesaj: $sender")
            .setContentText(messageText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(mainPending)
            .setAutoCancel(true)
            .addAction(action)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // use random ID to avoid overwriting all msgs
        notificationManager.notify(sender.hashCode(), builder.build())
    }
}
