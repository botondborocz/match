@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.ttproject.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import org.ttproject.config.BuildKonfig

// Native iOS Imports
import cocoapods.GoogleSignIn.GIDConfiguration
import cocoapods.GoogleSignIn.GIDSignIn
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

class IOSGoogleAuthClient : GoogleAuthClient {

    override suspend fun signIn(): String? = suspendCancellableCoroutine { continuation ->

        // 1. Configure the Google Auth SDK first
        val config = GIDConfiguration(
            clientID = BuildKonfig.IOS_CLIENT_ID,
            serverClientID = BuildKonfig.WEB_CLIENT_ID
        )
        GIDSignIn.sharedInstance.configuration = config

        // 2. The Bulletproof way to find the Compose Root View Controller
        var rootViewController: UIViewController? = null

        // Loop through all connected scenes
        val scenes = UIApplication.sharedApplication.connectedScenes
        for (scene in scenes) {
            val windowScene = scene as? UIWindowScene
            if (windowScene != null) {
                // Loop through all windows in the scene
                for (window in windowScene.windows) {
                    val uiWindow = window as? UIWindow
                    // Check if it's the main key window and has a root controller
                    if (uiWindow != null && uiWindow.isKeyWindow()) {
                        rootViewController = uiWindow.rootViewController
                        break
                    }
                }
            }
            if (rootViewController != null) break
        }

        // Fallback for older iOS versions
        if (rootViewController == null) {
            rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        }

        // If we still can't find it, we must abort cleanly so the app doesn't crash
        if (rootViewController == null) {
            println("CRITICAL ERROR: Could not find iOS Root View Controller.")
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        // 3. Launch the native iOS Google Sign-In modal
        GIDSignIn.sharedInstance.signInWithPresentingViewController(rootViewController) { result, error ->

            // Handle Errors
            if (error != null) {
                println("Google Sign In failed: ${error.localizedDescription}")
                continuation.resume(null)
                return@signInWithPresentingViewController
            }

            // Handle empty result
            if (result == null) {
                continuation.resume(null)
                return@signInWithPresentingViewController
            }

            // 4. Success! Extract the ID Token
            val idToken = result.user.idToken?.tokenString
            continuation.resume(idToken)
        }
    }
}

@Composable
actual fun rememberGoogleAuthClient(): GoogleAuthClient {
    return remember { IOSGoogleAuthClient() }
}