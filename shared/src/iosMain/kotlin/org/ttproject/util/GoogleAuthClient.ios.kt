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
import platform.UIKit.UIColor
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

class IOSGoogleAuthClient : GoogleAuthClient {

    override suspend fun signIn(): String? = suspendCancellableCoroutine { continuation ->

        // 1. Configure the Google Auth SDK
        val config = GIDConfiguration(
            clientID = BuildKonfig.IOS_CLIENT_ID,
            serverClientID = BuildKonfig.WEB_CLIENT_ID
        )
        GIDSignIn.sharedInstance.configuration = config

        // 2. Find the Root View Controller
        var rootViewController: UIViewController? = null
        val scenes = UIApplication.sharedApplication.connectedScenes
        for (scene in scenes) {
            val windowScene = scene as? UIWindowScene
            if (windowScene != null) {
                for (window in windowScene.windows) {
                    val uiWindow = window as? UIWindow
                    if (uiWindow != null && uiWindow.isKeyWindow()) {
                        rootViewController = uiWindow.rootViewController
                        break
                    }
                }
            }
            if (rootViewController != null) break
        }

        if (rootViewController == null) {
            rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        }

        if (rootViewController == null) {
            println("CRITICAL ERROR: Could not find iOS Root View Controller.")
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        // 3. Create a DUMMY invisible view controller to host the Google popup!
        // This prevents Compose Multiplatform rendering engines from conflicting with the secure web session.
        dispatch_async(dispatch_get_main_queue()) {
            val dummyController = UIViewController()
            dummyController.view.backgroundColor = UIColor.clearColor
            dummyController.modalPresentationStyle = platform.UIKit.UIModalPresentationOverFullScreen

            // Present the dummy controller on top of Compose
            rootViewController.presentViewController(dummyController, animated = false) {

                // Now, tell Google to present its popup on the DUMMY controller
                GIDSignIn.sharedInstance.signInWithPresentingViewController(dummyController) { result, error ->

                    // Immediately dismiss our dummy controller when Google is done
                    dummyController.dismissViewControllerAnimated(false, completion = null)

                    if (error != null) {
                        println("Google Sign In failed: ${error.localizedDescription}")
                        continuation.resume(null)
                        return@signInWithPresentingViewController
                    }

                    if (result == null) {
                        continuation.resume(null)
                        return@signInWithPresentingViewController
                    }

                    val idToken = result.user.idToken?.tokenString
                    continuation.resume(idToken)
                }
            }
        }
    }
}

@Composable
actual fun rememberGoogleAuthClient(): GoogleAuthClient {
    return remember { IOSGoogleAuthClient() }
}