package org.ttproject.util

import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSISO8601DateFormatter
import platform.Foundation.NSISO8601DateFormatWithInternetDateTime
import platform.Foundation.NSISO8601DateFormatWithFractionalSeconds
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale

actual fun formatMessageTime(isoTimestamp: String): String {
    return try {
        val isoFormatter = NSISO8601DateFormatter()

        // 1. Try parsing standard ISO8601 (e.g., 2026-03-29T14:00:00Z)
        isoFormatter.formatOptions = NSISO8601DateFormatWithInternetDateTime
        var date = isoFormatter.dateFromString(isoTimestamp)

        // 2. If it failed, try again WITH fractional seconds (e.g., 2026-03-29T14:00:00.123Z)
        if (date == null) {
            isoFormatter.formatOptions = NSISO8601DateFormatWithInternetDateTime or NSISO8601DateFormatWithFractionalSeconds
            date = isoFormatter.dateFromString(isoTimestamp)
        }

        // 3. If it STILL failed (bad data), just return the raw string
        if (date == null) return isoTimestamp

        // 4. Format to local user time (e.g. "4:00 PM" or "16:00")
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