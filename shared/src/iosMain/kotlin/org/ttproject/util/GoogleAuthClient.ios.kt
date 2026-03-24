@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.ttproject.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import org.ttproject.config.BuildKonfig

// Native iOS Imports (Managed by CocoaPods)
import cocoapods.GoogleSignIn.GIDConfiguration
import cocoapods.GoogleSignIn.GIDSignIn
import platform.UIKit.UIApplication

class IOSGoogleAuthClient : GoogleAuthClient {

    override suspend fun signIn(): String? = suspendCancellableCoroutine { continuation ->

        // 👇 ADD THESE DEBUG PRINTS HERE 👇
        println("====== GOOGLE SIGN-IN DEBUG ======")
        println("IOS_CLIENT_ID length: ${BuildKonfig.IOS_CLIENT_ID.length}")
        println("IOS_CLIENT_ID value: '${BuildKonfig.IOS_CLIENT_ID}'")
        println("WEB_CLIENT_ID length: ${BuildKonfig.WEB_CLIENT_ID.length}")
        println("WEB_CLIENT_ID value: '${BuildKonfig.WEB_CLIENT_ID}'")
        println("==================================")
        // 👆 ---------------------------- 👆

        // 1. Get the root view controller to present the native iOS popup
        val window = UIApplication.sharedApplication.keyWindow
        val rootViewController = window?.rootViewController

        if (rootViewController == null) {
            println("Failed to find root view controller")
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        // 2. Configure the Google Auth SDK using BuildKonfig values
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