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
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

class IOSGoogleAuthClient : GoogleAuthClient {

    override suspend fun signIn(): String? = suspendCancellableCoroutine { continuation ->

        println("====== GOOGLE SIGN-IN DEBUG ======")
        println("IOS_CLIENT_ID value: '${BuildKonfig.IOS_CLIENT_ID}'")
        println("WEB_CLIENT_ID value: '${BuildKonfig.WEB_CLIENT_ID}'")
        println("==================================")

        // 1. Get the ACTIVE window safely for modern iOS
        val windowScene = UIApplication.sharedApplication.connectedScenes.firstOrNull {
            (it as? UIWindowScene)?.activationState == platform.UIKit.UISceneActivationStateForegroundActive
        } as? UIWindowScene

        // Map through the windows list, cast them, and find the key window
        val activeWindow = windowScene?.windows?.mapNotNull { it as? UIWindow }?.firstOrNull { it.isKeyWindow() }
            ?: UIApplication.sharedApplication.keyWindow

        val rootViewController = activeWindow?.rootViewController

        if (rootViewController == null) {
            println("Failed to find root view controller")
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        // 2. Configure the Google Auth SDK
        val config = GIDConfiguration(
            clientID = BuildKonfig.IOS_CLIENT_ID,
            serverClientID = BuildKonfig.WEB_CLIENT_ID
        )
        GIDSignIn.sharedInstance.configuration = config

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