package org.ttproject.util

import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle

actual fun triggerRefreshHaptic() {
    // This creates that premium, subtle physical "pop" in the iPhone hardware
    val generator = UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)
    generator.prepare()
    generator.impactOccurred()
}