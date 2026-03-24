package org.ttproject.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import org.ttproject.config.BuildKonfig


// ⚠️ Note: These imports will be RED on Windows. Ignore them!
import cocoapods.GoogleSignIn.GIDConfiguration
import cocoapods.GoogleSignIn.GIDSignIn
import org.ttproject.util.GoogleAuthClient
import platform.UIKit.UIApplication

class IOSGoogleAuthClient : GoogleAuthClient {

    override suspend fun signIn(): String? = suspendCancellableCoroutine { continuation ->
        // 1. We need the root view controller to present the native iOS popup
        val window = UIApplication.sharedApplication.keyWindow
        val rootViewController = window?.rootViewController

        if (rootViewController == null) {
            println("Failed to find root view controller")
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        // 2. Configure the Google Auth SDK
        // Replace these with your actual IDs from Google Cloud Console
        val config = GIDConfiguration(
            clientID = BuildKonfig.IOS_CLIENT_ID,
            serverClientID = BuildKonfig.WEB_CLIENT_ID
        )
        GIDSignIn.sharedInstance.configuration = config

        // 3. Launch the native iOS Google Sign-In modal
        GIDSignIn.sharedInstance.signInWithPresentingViewController(rootViewController) { result, error ->

            // Handle Errors (e.g., user closed the popup)
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

            // 4. Success! Extract the ID Token (This is what you send to your Ktor backend)
            val idToken = result.user.idToken?.tokenString
            continuation.resume(idToken)
        }
    }
}

@Composable
actual fun rememberGoogleAuthClient(): GoogleAuthClient {
    return remember { IOSGoogleAuthClient() }
}