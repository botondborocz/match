package org.ttproject.util

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@RequiresApi(Build.VERSION_CODES.O)
actual fun formatMessageTime(isoTimestamp: String): String {
    return try {
        // Parse the UTC database string
        val instant = Instant.parse(isoTimestamp)

        // Convert to the user's local timezone
        val localTime = instant.atZone(ZoneId.systemDefault())

        // Format it using the Android system's short time preference (12h/24h aware)
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(localTime)
    } catch (e: Exception) {
        // Fallback to the raw string if the database sends a weird format
        isoTimestamp
    }
}