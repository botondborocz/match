package org.ttproject.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindowScene
import cocoapods.GoogleSignIn.GIDSignIn
import cocoapods.GoogleSignIn.GIDConfiguration
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
class IosGoogleAuthClient(
    private val clientId: String
) : GoogleAuthClient {

    override suspend fun signIn(): String? =
        // 1. FORCE THIS TO RUN ON THE iOS MAIN THREAD
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->

                if (clientId.isBlank()) {
                    println("Error: Google Client ID is empty!")
                    continuation.resume(null)
                    return@suspendCancellableCoroutine
                }

                GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID = clientId)

                // 2. GET THE TOPMOST VIEW CONTROLLER
                val topViewController = getTopViewController()

                if (topViewController == null) {
                    println("Error: Could not find a UIViewController to present on.")
                    continuation.resume(null)
                    return@suspendCancellableCoroutine
                }

                // Trigger the native iOS Google Sign-In
                GIDSignIn.sharedInstance.signInWithPresentingViewController(
                    presentingViewController = topViewController
                ) { result, error ->
                    if (error != null) {
                        println("Google Sign-In failed: ${error.localizedDescription}")
                        continuation.resume(null)
                    } else {
                        continuation.resume(result?.user?.idToken?.tokenString)
                    }
                }
            }
        }

    /**
     * Recursively finds the topmost UIViewController currently on screen.
     */
    private fun getTopViewController(): UIViewController? {
        val window = UIApplication.sharedApplication.connectedScenes
            .mapNotNull { it as? UIWindowScene }
            .flatMap { it.windows }
            .firstOrNull { it.isKeyWindow() }
            ?: UIApplication.sharedApplication.keyWindow

        var topController = window?.rootViewController

        // Drill down to the topmost presented view controller
        while (topController?.presentedViewController != null) {
            topController = topController.presentedViewController
        }

        return topController
    }
}

@Composable
actual fun rememberGoogleAuthClient(): GoogleAuthClient {
    // You can fetch this from BuildKonfig or hardcode it for now
    val iosClientId = BuildKonfig.IOS_CLIENT_ID

    return remember { IosGoogleAuthClient(clientId = iosClientId) }
}