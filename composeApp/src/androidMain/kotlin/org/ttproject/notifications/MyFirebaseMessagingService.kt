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
import org.ttproject.util.NotificationEventBus
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.Lifecycle

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val chatId = remoteMessage.data["chatId"] ?: return
        val senderName = remoteMessage.data["senderName"] ?: "New Message"
        val text = remoteMessage.data["text"] ?: ""

        // 1. ALWAYS trigger the UI refresh!
        // If they are staring at the chat screen, it will instantly pop up.
        NotificationEventBus.triggerRefresh()

        // 👇 2. THE FIX: Check if the app is currently in the foreground
        val isAppInForeground = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

        if (isAppInForeground) {
            // The user is actively using the app!
            // We already refreshed the UI, so exit early and do NOT show a system banner.
            return
        }

        // ---------------------------------------------------------
        // 👇 Everything below this line ONLY runs if the app is backgrounded/closed
        // ---------------------------------------------------------

        // 3. Check Permissions (Prevents crashes on Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return // User hasn't granted permission yet, fail silently
            }
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "chat_messages"

        // Create Channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Chat Messages", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        // Create the Deep Link Intent
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("chatId", chatId)
        }

        val safeNotificationId = abs(chatId.hashCode())

        val pendingIntent = PendingIntent.getActivity(
            this,
            safeNotificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        var messagingStyle: NotificationCompat.MessagingStyle? = null

        // Querying active notifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activeNotifications = notificationManager.activeNotifications
            for (activeNotification in activeNotifications) {
                if (activeNotification.id == safeNotificationId) {
                    messagingStyle = NotificationCompat.MessagingStyle
                        .extractMessagingStyleFromNotification(activeNotification.notification)
                    break
                }
            }
        }

        if (messagingStyle == null) {
            messagingStyle = NotificationCompat.MessagingStyle(Person.Builder().setName("Me").build())
        }

        val sender = Person.Builder().setName(senderName).build()
        messagingStyle.addMessage(text, Date().time, sender)

        val fullColorLogo = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setLargeIcon(fullColorLogo)
            .setStyle(messagingStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(safeNotificationId, notification)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        println("🔄 Background FCM Token refreshed: $token")
    }
}