package org.ttproject.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.interop.LocalUIViewController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import platform.UIKit.UIViewController
import platform.Foundation.NSBundle
import platform.Foundation.NSError
import cocoapods.GoogleSignIn.GIDSignIn
import cocoapods.GoogleSignIn.GIDConfiguration
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
class IosGoogleAuthClient(
    private val viewController: UIViewController
) : GoogleAuthClient {

    override suspend fun signIn(): String? =
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->

                // 1. Read the Client ID dynamically from the Info.plist
                val clientId = NSBundle.mainBundle.objectForInfoDictionaryKey("GIDClientID") as? String

                if (clientId.isNullOrBlank()) {
                    val errorMsg = "Info.plist Error: GIDClientID is missing or empty!"
                    continuation.resumeWithException(IllegalStateException(errorMsg))
                    return@suspendCancellableCoroutine
                }

                // 2. Configure the Google SDK
                GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID = clientId)

                // 3. Trigger the native Google Sign-In sheet
                GIDSignIn.sharedInstance.signInWithPresentingViewController(
                    presentingViewController = viewController
                ) { result, error ->

                    if (error != null) {
                        // Extract the exact iOS error code and message
                        val nsError = error as? NSError
                        val errorMsg = "Google SDK Error: ${error.localizedDescription} (Code: ${nsError?.code})"

                        // Throw the exception so Compose can catch it!
                        continuation.resumeWithException(RuntimeException(errorMsg))
                    } else {
                        // Success! Extract the token.
                        val token = result?.user?.idToken?.tokenString
                        if (token != null) {
                            continuation.resume(token)
                        } else {
                            continuation.resumeWithException(RuntimeException("Login succeeded, but Google returned a null ID Token."))
                        }
                    }
                }
            }
        }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberGoogleAuthClient(): GoogleAuthClient {
    // Grab the exact UIViewController powering this Compose screen
    val viewController = LocalUIViewController.current

    // Remember the client, passing in the view controller
    return remember(viewController) {
        IosGoogleAuthClient(viewController = viewController)
    }
}