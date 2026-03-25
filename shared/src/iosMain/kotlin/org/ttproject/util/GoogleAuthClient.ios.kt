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
                    // Throw an error instead of returning null
                    continuation.resumeWith(Result.failure(Exception("ERROR: Client ID is blank. Did you replace the placeholder?")))
                    return@suspendCancellableCoroutine
                }

                GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID = clientId)

                GIDSignIn.sharedInstance.signInWithPresentingViewController(
                    presentingViewController = viewController
                ) { result, error ->
                    if (error != null) {
                        // Extract the exact error and THROW it
                        val nsError = error as? platform.Foundation.NSError
                        val errorMsg = "Google SDK Error: ${error.localizedDescription} (Code: ${nsError?.code})"

                        continuation.resumeWith(Result.failure(Exception(errorMsg)))
                    } else {
                        val token = result?.user?.idToken?.tokenString
                        if (token != null) {
                            continuation.resume(token)
                        } else {
                            continuation.resumeWith(Result.failure(Exception("ERROR: Login succeeded but ID Token was missing!")))
                        }
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
    val iosClientId = "115244117318-35pj0hqg5ko98esh44q6i9gn7t3e7vae.apps.googleusercontent.com"

    // Grab the exact UIViewController powering this Compose screen
    val viewController = LocalUIViewController.current

    return remember(viewController) {
        IosGoogleAuthClient(clientId = iosClientId, viewController = viewController)
    }
}