package org.ttproject.util

import java.util.Locale

actual fun changePlatformLanguage(languageCode: String) {
    val newLocale = Locale(languageCode)
    Locale.setDefault(newLocale)
}