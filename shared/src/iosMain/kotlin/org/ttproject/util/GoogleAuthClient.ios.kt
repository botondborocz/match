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
import platform.UIKit.UIWindow
import cocoapods.GoogleSignIn.GIDSignIn
import cocoapods.GoogleSignIn.GIDConfiguration
import kotlinx.cinterop.ExperimentalForeignApi
import org.ttproject.config.BuildKonfig

@OptIn(ExperimentalForeignApi::class)
class IosGoogleAuthClient(
    private val clientId: String
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

                val topViewController = getTopViewController()

                if (topViewController == null) {
                    println("Error: Could not find a UIViewController to present on.")
                    continuation.resume(null)
                    return@suspendCancellableCoroutine
                }

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

    private fun getTopViewController(): UIViewController? {
        // 1. Get the scenes, cast to UIWindowScene
        val scenes = UIApplication.sharedApplication.connectedScenes
            .mapNotNull { it as? UIWindowScene }

        // 2. Get the windows, explicitly cast the generic list elements to UIWindow
        val window = scenes.flatMap { it.windows }
            .mapNotNull { it as? UIWindow }
            .firstOrNull { it.isKeyWindow() }
            ?: UIApplication.sharedApplication.keyWindow

        // 3. Drill down to the topmost presented view controller
        var topController = window?.rootViewController

        while (topController?.presentedViewController != null) {
            topController = topController?.presentedViewController
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