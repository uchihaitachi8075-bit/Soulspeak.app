package com.example.soulspeak

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

import android.util.Log

class SoulspeakMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "Message Received from: ${remoteMessage.from}")

        // Update Global Notification Count
        NotificationState.increment()

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d("FCM", "Notification received: ${it.title} - ${it.body}")
            showNotification(it.title ?: "Soulspeak", it.body ?: "")
        }

        // Check data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d("FCM", "Data payload received: ${remoteMessage.data}")
            val title = remoteMessage.data["title"] ?: "New Confession"
            val body = remoteMessage.data["body"] ?: "Check out what's new!"
            showNotification(title, body)
        }
    }

    private fun showNotification(title: String, message: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "soulspeak_notifications"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Default icon for testing
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX) // Max priority
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Sound and vibration

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Soulspeak Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Confession and room updates"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }

    override fun onNewToken(token: String) {
        // Here you would send the token to your server if needed
    }
}
