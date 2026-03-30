package org.ttproject.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.Date
import kotlin.math.abs
import org.ttproject.MainActivity
import org.ttproject.R

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val chatId = remoteMessage.data["chatId"] ?: return
        val senderName = remoteMessage.data["senderName"] ?: "New Message"
        val text = remoteMessage.data["text"] ?: ""

        NotificationEventBus.triggerRefresh()

        // 1. Check Permissions (Prevents crashes on Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return // User hasn't granted permission yet, fail silently
            }
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "chat_messages"

        // 2. Create Channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Chat Messages", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        // 3. Create the Deep Link Intent
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("chatId", chatId)
        }

        // Use an absolute value to prevent Samsung from blocking negative IDs
        val safeNotificationId = abs(chatId.hashCode())

        val pendingIntent = PendingIntent.getActivity(
            this,
            safeNotificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 👇 4. THE FIX: Look for an existing notification to grab its history!
        var messagingStyle: NotificationCompat.MessagingStyle? = null

        // Querying active notifications requires Android 6.0 (API 23+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activeNotifications = notificationManager.activeNotifications
            for (activeNotification in activeNotifications) {
                if (activeNotification.id == safeNotificationId) {
                    // We found it! Extract the old messages.
                    messagingStyle = NotificationCompat.MessagingStyle
                        .extractMessagingStyleFromNotification(activeNotification.notification)
                    break
                }
            }
        }

        // If there is no existing notification (or it was swiped away), start a fresh one
        if (messagingStyle == null) {
            messagingStyle = NotificationCompat.MessagingStyle(Person.Builder().setName("Me").build())
        }

        // Add the NEW message to the bottom of the list
        val sender = Person.Builder().setName(senderName).build()
        messagingStyle.addMessage(text, Date().time, sender)


        // 👇 5. Grab your full-color app icon
        val fullColorLogo = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)

        // 👇 6. Build the final notification using the stacked style!
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setLargeIcon(fullColorLogo)
            .setStyle(messagingStyle) // 👈 Now contains the whole history!
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // 7. Show it!
        notificationManager.notify(safeNotificationId, notification)


        // 👇 1. Check if it's a Xiaomi device (including POCO and Redmi!)
//        val isXiaomi = Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true) ||
//                Build.MANUFACTURER.equals("POCO", ignoreCase = true) ||
//                Build.MANUFACTURER.equals("Redmi", ignoreCase = true)
//
//        val builder = NotificationCompat.Builder(this, channelId)
//            .setPriority(NotificationCompat.PRIORITY_HIGH)
//            .setAutoCancel(true)
//            .setContentIntent(pendingIntent)
//
//        if (isXiaomi) {
//            // 🚨 XIAOMI MODE: Use standard layout with full-color icons!
//            val fullColorLogo = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
//
//            builder.setSmallIcon(R.mipmap.ic_launcher) // MIUI actively prefers the colored icon here
//                .setLargeIcon(fullColorLogo)           // Put the logo on the left
//                .setContentTitle(senderName)           // Standard title
//                .setContentText(text)                  // Standard text
//            // Notice we DO NOT attach .setStyle(messagingStyle) here!
//        } else {
//            // 🚨 EVERYONE ELSE (Samsung, Pixel): Use standard Android rules and Chat styling
//            val sender = Person.Builder().setName(senderName).build()
//            val messagingStyle = NotificationCompat.MessagingStyle(Person.Builder().setName("Me").build())
//                .addMessage(text, Date().time, sender)
//
//            builder.setSmallIcon(R.drawable.ic_stat_name) // Strict white silhouette
//                .setStyle(messagingStyle)                 // Fancy chat avatars
//        }

//        notificationManager.notify(safeNotificationId, builder.build())
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        println("🔄 Background FCM Token refreshed: $token")
    }
}