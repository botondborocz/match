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
            suspendCancellableCoroutine { continuation ->

                if (clientId.isBlank()) {
                    println("Error: Google Client ID is empty!")
                    continuation.resume(null)
                    return@suspendCancellableCoroutine
                }

                GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID = clientId)

                // Trigger the native iOS Google Sign-In
                GIDSignIn.sharedInstance.signInWithPresentingViewController(
                    presentingViewController = viewController
                ) { result, error ->
                    if (error != null) {
                        // Cast to NSError to get the exact numeric error code
                        val nsError = error as? platform.Foundation.NSError
                        val errorMessage = "🚨 GOOGLE SIGN-IN FAILED: ${error.localizedDescription} | Domain: ${nsError?.domain} | Code: ${nsError?.code}"

                        println(errorMessage)

                        // Let's intentionally crash the app here so the error is forced onto your screen/logs!
                        throw RuntimeException(errorMessage)
                    } else {
                        continuation.resume(result?.user?.idToken?.tokenString)
                    }
                }
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