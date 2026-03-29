package org.ttproject.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.Date

class MyFirebaseMessagingService : FirebaseMessagingService() {

    // This fires when a Data payload arrives (even if the app is closed!)
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // 1. Extract the raw data sent from your Ktor server
        val chatId = remoteMessage.data["chatId"] ?: return
        val senderName = remoteMessage.data["senderName"] ?: "Unknown"
        val text = remoteMessage.data["text"] ?: ""

        showGroupedNotification(chatId, senderName, text)
    }

    private fun showGroupedNotification(chatId: String, senderName: String, text: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "chat_messages"

        // Create the High Priority channel (required for Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Chat Messages", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        // 2. THE WHATSAPP MAGIC: NotificationCompat.MessagingStyle
        val sender = Person.Builder().setName(senderName).build()
        val messagingStyle = NotificationCompat.MessagingStyle(Person.Builder().setName("Me").build())
            .addMessage(text, Date().time, sender)

        // 3. THE GROUPING MAGIC: Use the Chat ID as the Notification ID
        // Because the ID is the same for this specific chat, Android will append
        // new messages to the existing bracket instead of making a new one!
        val notificationId = chatId.hashCode()

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // TODO: Swap with your app's actual icon!
            .setStyle(messagingStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Forces it to drop down from the top
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    // Optional but good practice: If Google refreshes the token in the background, catch it
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        println("🔄 Background FCM Token refreshed: $token")
        // In the future, you can send this fresh token to your Ktor server here
    }
}