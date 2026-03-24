package org.ttproject.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import cocoapods.GoogleSignIn.GIDSignIn
import cocoapods.GoogleSignIn.GIDConfiguration // <-- Add this import
import kotlinx.cinterop.ExperimentalForeignApi
import org.ttproject.config.BuildKonfig

@OptIn(ExperimentalForeignApi::class)
class IosGoogleAuthClient(
    private val clientId: String
) : GoogleAuthClient {

    override suspend fun signIn(): String? = suspendCancellableCoroutine { continuation ->
        // 1. Failsafe check: Ensure the string isn't empty
        if (clientId.isBlank()) {
            println("Error: Google Client ID is empty! Please check your configuration.")
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        // 2. Configure the SDK immediately before calling sign-in
        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID = clientId)

        val rootViewController = getRootViewController()

        if (rootViewController == null) {
            println("Error: Could not find root UIViewController.")
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        GIDSignIn.sharedInstance.signInWithPresentingViewController(
            presentingViewController = rootViewController
        ) { result, error ->
            if (error != null) {
                println("Google Sign-In failed: ${error.localizedDescription}")
                continuation.resume(null)
            } else {
                continuation.resume(result?.user?.idToken?.tokenString)
            }
        }
    }

    private fun getRootViewController(): UIViewController? {
        val scenes = UIApplication.sharedApplication.connectedScenes
        val windowScene = scenes.firstOrNull { it is UIWindowScene } as? UIWindowScene

        val window = windowScene?.windows?.firstOrNull { (it as UIWindow).isKeyWindow() } as? UIWindow
            ?: UIApplication.sharedApplication.keyWindow

        return window?.rootViewController
    }
}

@Composable
actual fun rememberGoogleAuthClient(): GoogleAuthClient {
    // You can fetch this from BuildKonfig or hardcode it for now
    val iosClientId = BuildKonfig.IOS_CLIENT_ID

    return remember { IosGoogleAuthClient(clientId = iosClientId) }
}