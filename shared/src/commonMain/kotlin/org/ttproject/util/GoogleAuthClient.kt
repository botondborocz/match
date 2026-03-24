package org.ttproject.util

import androidx.compose.runtime.Composable

// An interface to handle the Native Google Login flow
interface GoogleAuthClient {
    // Triggers the native UI popup and returns the Google ID Token on success, or null on failure/cancel
    suspend fun signIn(): String?
}

// A Composable function to provide the correct implementation for the current platform
@Composable
expect fun rememberGoogleAuthClient(): GoogleAuthClient