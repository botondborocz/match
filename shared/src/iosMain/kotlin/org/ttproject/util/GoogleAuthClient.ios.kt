package org.ttproject.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.interop.LocalUIViewController // <-- The magic Compose import
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import platform.UIKit.UIViewController
import cocoapods.GoogleSignIn.GIDSignIn
import cocoapods.GoogleSignIn.GIDConfiguration
import kotlinx.cinterop.ExperimentalForeignApi
import org.ttproject.config.BuildKonfig

@OptIn(ExperimentalForeignApi::class)
class IosGoogleAuthClient(
    private val clientId: String,
    private val viewController: UIViewController // <-- Pass this in
) : GoogleAuthClient {

    override suspend fun signIn(): String? =
        withContext(Dispatchers.Main) {
            println("🔵 STEP 1: signIn() function triggered on Main Thread")

            suspendCancellableCoroutine { continuation ->
                println("🔵 STEP 2: Inside suspendCancellableCoroutine")

                if (clientId.isBlank()) {
                    println("🔴 STEP 3 ERROR: Client ID is completely blank!")
                    continuation.resume(null)
                    return@suspendCancellableCoroutine
                }

                println("🔵 STEP 4: Configuring Google SDK with ID: $clientId")
                GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID = clientId)

                println("🔵 STEP 5: Requesting presentation on ViewController: $viewController")

                // Track if the coroutine gets cancelled prematurely
                continuation.invokeOnCancellation {
                    println("🔴 COROUTINE CANCELLED! The UI cancelled the login before it could finish.")
                }

                println("🔵 STEP 6: Firing Google SDK signInWithPresentingViewController...")

                GIDSignIn.sharedInstance.signInWithPresentingViewController(
                    presentingViewController = viewController
                ) { result, error ->

                    println("🔵 STEP 7: Google SDK Callback actually fired!")

                    if (error != null) {
                        val nsError = error as? platform.Foundation.NSError
                        println("🔴 STEP 8 ERROR: ${error.localizedDescription} | Code: ${nsError?.code}")
                        continuation.resume(null)
                    } else {
                        val token = result?.user?.idToken?.tokenString
                        println("🟢 STEP 8 SUCCESS: Token received!")
                        continuation.resume(token)
                    }
                }

                println("🔵 STEP 9: SDK called successfully, waiting for user to log in...")
            }
        }

    // Notice: We completely deleted the getTopViewController() function!
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberGoogleAuthClient(): GoogleAuthClient {
    // You can fetch this from BuildKonfig or hardcode it for now
    val iosClientId = BuildKonfig.IOS_CLIENT_ID

    // Grab the exact UIViewController powering this Compose screen
    val viewController = LocalUIViewController.current

    return remember(viewController) {
        IosGoogleAuthClient(clientId = iosClientId, viewController = viewController)
    }
}