package org.ttproject.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import org.ttproject.AppColors

// 👇 Import your generated KMP resources here
import ttproject.composeapp.generated.resources.Res
import ttproject.composeapp.generated.resources.match_logo
import ttproject.composeapp.generated.resources.match_logo_long

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileTopBar() { // We don't need the 'title' parameter anymore
    TopAppBar(
        title = {
            // Replace the Text with your PNG Image
            Image(
                painter = painterResource(Res.drawable.match_logo_long),
                contentDescription = "App Logo",
                modifier = Modifier.height(28.dp) // Adjust this to make your logo look crisp
            )
        },
        // 👇 Removed the navigationIcon entirely!
        actions = {
            // User Avatar keeps the UI balanced
            Box(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(AppColors.Background),
                contentAlignment = Alignment.Center
            ) {
                Text("JD", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AppColors.SurfaceDark
        )
    )
}