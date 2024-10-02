package com.adair.lookoot

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {
    // Tag for logging
    private val tag = "MyFirebaseMsgService"

    // Called when a new message is received from FCM.
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(tag, "From: ${remoteMessage.from}")

        // Check if message contains a data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(tag, "Message data payload: ${remoteMessage.data}")

            // Handle the data payload
            handleDataMessage(remoteMessage.data)
        }

        // Check if message contains a notification payload
        remoteMessage.notification?.let {
            Log.d(tag, "Message Notification Body: ${it.body}")
            sendNotification(it.title, it.body)
        }
    }

    private fun handleDataMessage(data: Map<String, String>) {
        //TO DO
        // storing information in a database, or triggering a background task.
        val title = data["title"]
        val message = data["message"]
        if (title != null && message != null) {
            sendNotification(title, message)
        }
    }

    // Called when the FCM token is refreshed.
    override fun onNewToken(token: String) {
        Log.d(tag, "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    // Sends the FCM registration token to the server (to be implemented).
    private fun sendRegistrationToServer(token: String?) {
        //TO DO
        Log.d(tag, "sendRegistrationTokenToServer($token)")
    }
    // Builds and displays a notification with the given title and message.
    private fun sendNotification(title: String?, messageBody: String?) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
            PendingIntent.FLAG_IMMUTABLE)

        val channelId = "fcm_default_channel"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.login_image)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        val channel = NotificationChannel(channelId,
            "FCM Notifications",
            NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)

        // Displays the notification.
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
        Log.d(tag, "Notification sent. ID: $notificationId, Title: $title, Body: $messageBody")
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }
}