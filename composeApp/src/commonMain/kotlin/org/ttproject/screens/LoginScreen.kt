package org.ttproject.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.ttproject.AppColors
import org.ttproject.isDark
import org.ttproject.shared.resources.continue_with_google
import org.ttproject.shared.resources.forgot_password
import org.ttproject.shared.resources.login
import org.ttproject.shared.resources.or
import org.ttproject.viewmodel.LoginState
import org.ttproject.viewmodel.LoginViewModel
import ttproject.composeapp.generated.resources.Res
import org.ttproject.shared.resources.Res as SharedRes
import ttproject.composeapp.generated.resources.match_logo_long
import ttproject.composeapp.generated.resources.match_logo_long_dark
import org.ttproject.shared.resources.password
import org.ttproject.util.rememberGoogleAuthClient

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = koinViewModel(),
    onLoginSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current

    val scope = rememberCoroutineScope()
    val googleAuthClient = rememberGoogleAuthClient()
    var isGoogleLoading by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is LoginState.Success) {
            isGoogleLoading = false // Stop spinner
            onLoginSuccess()
            viewModel.resetState()
        } else if (uiState is LoginState.Error) {
            isGoogleLoading = false // Stop spinner if backend fails
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // 👇 1. Replaced hardcoded gradient with dynamic background
            .background(AppColors.Background)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isDark) {
                Image(
                    painter = painterResource(Res.drawable.match_logo_long),
                    contentDescription = "App Logo",
                    modifier = Modifier.height(64.dp)
                )
            }
            else {
                Image(
                    painter = painterResource(Res.drawable.match_logo_long_dark),
                    contentDescription = "App Logo",
                    modifier = Modifier.height(64.dp)
                )
            }
            Spacer(modifier = Modifier.height(48.dp))

            // --- INPUT FIELDS ---
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.AccentOrange,
                    // 👇 2. Mapped unfocused elements to TextGray
                    unfocusedBorderColor = AppColors.TextGray.copy(alpha = 0.5f),
                    focusedLabelColor = AppColors.AccentOrange,
                    unfocusedLabelColor = AppColors.TextGray,
                    // 👇 3. Mapped input text to TextPrimary
                    focusedTextColor = AppColors.TextPrimary,
                    unfocusedTextColor = AppColors.TextPrimary,
                    cursorColor = AppColors.AccentOrange
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(SharedRes.string.password)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.AccentOrange,
                    unfocusedBorderColor = AppColors.TextGray.copy(alpha = 0.5f),
                    focusedLabelColor = AppColors.AccentOrange,
                    unfocusedLabelColor = AppColors.TextGray,
                    focusedTextColor = AppColors.TextPrimary,
                    unfocusedTextColor = AppColors.TextPrimary,
                    cursorColor = AppColors.AccentOrange
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Forgot Password
            Text(
                text = stringResource(SharedRes.string.forgot_password),
                color = AppColors.AccentOrange,
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable { /* TODO: Navigate to recovery */ }
                    .padding(4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- STATE HANDLING (Error & Loading) ---
            AnimatedVisibility(visible = uiState is LoginState.Error) {
                if (uiState is LoginState.Error) {
                    val errorMessage = (uiState as LoginState.Error).message
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = AppColors.ErrorText, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(errorMessage, color = AppColors.ErrorText, fontSize = 14.sp)
                    }
                }
            }

            // --- PRIMARY LOGIN BUTTON ---
            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.login(email, password)
                },
                enabled = uiState !is LoginState.Loading && email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.AccentOrange,
                    disabledContainerColor = AppColors.AccentOrange.copy(alpha = 0.5f)
                )
            ) {
                if (uiState is LoginState.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    // Note: Kept text white here because white text on an orange button looks great in both themes!
                    Text(stringResource(SharedRes.string.login).uppercase(), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- DIVIDER ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 👇 4. Softened dividers for both themes
                HorizontalDivider(modifier = Modifier.weight(1f), color = AppColors.TextGray.copy(alpha = 0.3f))
                Text(" ${stringResource(SharedRes.string.or)} ", color = AppColors.TextGray, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 16.dp))
                HorizontalDivider(modifier = Modifier.weight(1f), color = AppColors.TextGray.copy(alpha = 0.3f))
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- GOOGLE LOGIN BUTTON ---
            OutlinedButton(
                onClick = {
                    focusManager.clearFocus()
                    isGoogleLoading = true
                    // 👇 2. Launch the native Google Sign-In flow!
                    scope.launch {
                        // This will suspend and wait for the user to finish the native Google pop-up
                        val idToken = googleAuthClient.signIn()

                        if (idToken != null) {
                            // If we got a token from Google, send it to our Ktor backend!
                            viewModel.googleLogin(idToken)
                        } else {
                            // The user canceled the popup, or it failed.
                            // You could optionally show a toast here.
                            isGoogleLoading = false
                            println("Google Sign-In canceled or failed")
                        }
                    }
                },
                enabled = !isGoogleLoading && uiState !is LoginState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, AppColors.TextGray.copy(alpha = 0.5f)), // 👇 5. Dynamic border
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent
                )
            ) {
                if (isGoogleLoading) {
                    CircularProgressIndicator(
                        color = AppColors.TextPrimary,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "G",
                        color = AppColors.TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(end = 12.dp)
                    )

                    Text(
                        text = stringResource(SharedRes.string.continue_with_google),
                        color = AppColors.TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}