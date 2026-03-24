package org.ttproject.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
// Note: This import requires you to expose the GoogleSignIn iOS framework
// to Kotlin, typically via the KMP CocoaPods plugin.
import cocoapods.GoogleSignIn.GIDSignIn

class IosGoogleAuthClient : GoogleAuthClient {

    override suspend fun signIn(): String? = suspendCancellableCoroutine { continuation ->
        val rootViewController = getRootViewController()

        if (rootViewController == null) {
            println("Error: Could not find root UIViewController to present Google Sign-In.")
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        // Trigger the native iOS Google Sign-In
        GIDSignIn.sharedInstance.signInWithPresentingViewController(
            presentingViewController = rootViewController
        ) { result, error ->
            if (error != null) {
                println("Google Sign-In failed: ${error.localizedDescription}")
                continuation.resume(null)
            } else {
                // Extract the ID token on success
                val idToken = result?.user?.idToken?.tokenString
                continuation.resume(idToken)
            }
        }
    }

    /**
     * Helper function to find the currently active UIViewController
     * in the iOS application to present the Google Sign-In overlay.
     */
    private fun getRootViewController(): UIViewController? {
        val scenes = UIApplication.sharedApplication.connectedScenes
        val windowScene = scenes.firstOrNull { it is UIWindowScene } as? UIWindowScene

        val window = windowScene?.windows?.firstOrNull { (it as UIWindow).isKeyWindow() } as? UIWindow
            ?: UIApplication.sharedApplication.keyWindow // Fallback for older iOS versions

        return window?.rootViewController
    }
}

@Composable
actual fun rememberGoogleAuthClient(): GoogleAuthClient {
    // Remember the instance so it survives Compose recompositions
    return remember { IosGoogleAuthClient() }
}