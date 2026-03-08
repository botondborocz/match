package org.ttproject.util

import platform.Foundation.NSUserDefaults

actual fun changePlatformLanguage(languageCode: String) {
    NSUserDefaults.standardUserDefaults.setObject(
        arrayListOf(languageCode),
        "AppleLanguages"
    )
    NSUserDefaults.standardUserDefaults.synchronize()
}