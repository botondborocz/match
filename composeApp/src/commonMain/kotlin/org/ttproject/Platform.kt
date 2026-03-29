package org.ttproject

fun isIosPlatform(): Boolean = getPlatform().name.contains("iOS", ignoreCase = true)