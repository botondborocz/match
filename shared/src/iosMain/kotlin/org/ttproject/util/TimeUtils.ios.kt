package org.ttproject.util

import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSISO8601DateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale

actual fun formatMessageTime(isoTimestamp: String): String {
    return try {
        // Parse the UTC database string
        val isoFormatter = NSISO8601DateFormatter()
        val date = isoFormatter.dateFromString(isoTimestamp) ?: return isoTimestamp

        // Format it using the iOS system's short time preference
        val displayFormatter = NSDateFormatter().apply {
            dateStyle = NSDateFormatterNoStyle
            timeStyle = NSDateFormatterShortStyle
            locale = NSLocale.currentLocale()
        }

        displayFormatter.stringFromDate(date)
    } catch (e: Exception) {
        isoTimestamp
    }
}