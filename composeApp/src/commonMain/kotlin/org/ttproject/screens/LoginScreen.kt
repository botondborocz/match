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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.ttproject.AppColors
import org.ttproject.shared.resources.continue_with_google
import org.ttproject.shared.resources.forgot_password
import org.ttproject.shared.resources.login
import org.ttproject.shared.resources.or
import org.ttproject.viewmodel.LoginState
import org.ttproject.viewmodel.LoginViewModel
import ttproject.composeapp.generated.resources.Res
import ttproject.composeapp.generated.resources.match_logo_long
import org.ttproject.shared.resources.Res as SharedRes
import org.ttproject.shared.resources.password

@Composable
fun LoginScreen(
    // 👇 Using koinViewModel() here so it survives screen rotations!
    viewModel: LoginViewModel = koinViewModel(),
    onLoginSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Used to hide the keyboard when tapping outside the text fields
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState) {
        if (uiState is LoginState.Success) {
            onLoginSuccess()
            viewModel.resetState()
        }
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF151C2C), Color(0xFF0A0D14)) // Deep dark blue to almost black
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
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

            // --- LOGO & HEADER ---
//            Text(
//                text = "MATCH",
//                color = Color.White,
//                fontSize = 42.sp,
//                fontWeight = FontWeight.ExtraBold,
//                letterSpacing = 2.sp
//            )
//            Spacer(modifier = Modifier.height(8.dp))
//            Text(
//                text = "Find your partner today",
//                color = Color.LightGray,
//                fontSize = 16.sp
//            )
            Image(
                painter = painterResource(Res.drawable.match_logo_long),
                contentDescription = "App Logo",
                modifier = Modifier.height(64.dp)
            )
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
                    unfocusedBorderColor = Color.DarkGray,
                    focusedLabelColor = AppColors.AccentOrange,
                    unfocusedLabelColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = AppColors.AccentOrange
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource( SharedRes.string.password)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(), // Hides the password text!
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.AccentOrange,
                    unfocusedBorderColor = Color.DarkGray,
                    focusedLabelColor = AppColors.AccentOrange,
                    unfocusedLabelColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = AppColors.AccentOrange
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Forgot Password (Placeholder)
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
                    Text(stringResource(SharedRes.string.login).uppercase(), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- DIVIDER ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.DarkGray)
                Text(" ${stringResource(SharedRes.string.or)} ", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 16.dp))
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.DarkGray)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- GOOGLE LOGIN BUTTON ---
            OutlinedButton(
                onClick = { /* TODO: Implement Google Auth */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, Color.DarkGray),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent
                )
            ) {
                // We use a simple text placeholder for the icon until you import the Google drawable
                Text(
                    text = "G",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Text(
                    text = stringResource(SharedRes.string.continue_with_google),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}