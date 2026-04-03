package org.ttproject.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.preat.peekaboo.image.picker.SelectionMode
import com.preat.peekaboo.image.picker.rememberImagePickerLauncher
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.ttproject.AppColors
import org.ttproject.AppColors.TextGray
import org.ttproject.shared.resources.backhand
import org.ttproject.shared.resources.blade
import org.ttproject.shared.resources.dark
import org.ttproject.shared.resources.forehand
import org.ttproject.shared.resources.language
import org.ttproject.shared.resources.light
import org.ttproject.shared.resources.logout
import org.ttproject.shared.resources.my_gear
import org.ttproject.shared.resources.system_default
import org.ttproject.shared.resources.theme
import org.ttproject.viewmodel.ProfileState
import org.ttproject.viewmodel.ProfileViewModel
import org.ttproject.util.ThemeMode
import org.ttproject.shared.resources.Res as SharedRes
import coil3.compose.AsyncImage
import org.ttproject.shared.resources.cancel
import org.ttproject.shared.resources.edit_profile
import org.ttproject.shared.resources.save
import org.ttproject.shared.resources.username

@Composable
fun ProfileScreen(
    currentLanguage: String = "en",
    currentThemeMode: ThemeMode = ThemeMode.System,
    onLogoutClick: () -> Unit = {},
    onChangeLanguage: (String) -> Unit = {},
    onChangeTheme: (ThemeMode) -> Unit = {},
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var isAvatarExpanded by remember { mutableStateOf(false) }

    // 👇 NEW: State for the Edit Profile Modal
    var isEditProfileModalOpen by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    var imageToUpload by remember { mutableStateOf<ByteArray?>(null) }

    val singleImagePicker = rememberImagePickerLauncher(
        selectionMode = SelectionMode.Single,
        scope = scope,
        onResult = { byteArrays ->
            byteArrays.firstOrNull()?.let { imageBytes ->
                println("✅ Image picked! Size: ${imageBytes.size} bytes")
                imageToUpload = imageBytes
            }
        }
    )

    if (imageToUpload != null) {
        AvatarFramerDialog(
            imageBytes = imageToUpload!!,
            onDismiss = { imageToUpload = null },
            onConfirm = { selectedBias ->
                viewModel.uploadProfileImage(imageToUpload!!)
                imageToUpload = null
            }
        )
    }

    LaunchedEffect(Unit) {
        if (viewModel.uiState.value !is ProfileState.Success) {
            viewModel.fetchUserProfile()
        }
    }

    when (uiState) {
        is ProfileState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppColors.AccentOrange)
            }
        }

        is ProfileState.Error -> {
            val error = (uiState as ProfileState.Error).message
            Text(text = error, color = Color.Red)
        }

        is ProfileState.Success -> {
            val userData = uiState as ProfileState.Success
            var animateTrigger by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                animateTrigger = true
            }

            LaunchedEffect(userData.language) {
                if (userData.language != null && userData.language != currentLanguage) {
                    onChangeLanguage(userData.language!!)
                }
            }

            val scrollState = rememberScrollState()

            // AVATAR PREVIEW DIALOG
            if (isAvatarExpanded) {
                AvatarPreviewDialog(
                    username = userData.name ?: "Player",
                    imageUrl = userData.imageUrl,
                    onDismissRequest = { isAvatarExpanded = false },
                    onEditClick = {
                        singleImagePicker.launch()
                        isAvatarExpanded = false
                    }
                )
            }

            // 👇 NEW: EDIT PROFILE DIALOG
            if (isEditProfileModalOpen) {
                EditProfileDialog(
                    initialName = userData.name ?: "",
                    initialBlade = userData.blade ?: "Butterfly Viscaria",
                    initialForehand = userData.rubberFh ?: "Tenergy 05",
                    initialBackhand = userData.rubberBh ?: "DHS Hurricane 3 Neo",
                    onDismiss = { isEditProfileModalOpen = false },
                    onSave = { newName, newBlade, newFh, newBh ->
                        // 👇 Call ViewModel to update data!
                        viewModel.updateProfile(newName, newBlade, newFh, newBh)
                        isEditProfileModalOpen = false
                    }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppColors.Background)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedVisibility(
                    visible = animateTrigger,
                    enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { 50 }
                ) {
                    Column {
                        ProfileHeader(
                            profileData = userData,
                            onAvatarClick = { isAvatarExpanded = true  },
                            onPhotoEditClick = { singleImagePicker.launch() },
                            // 👇 Pass callback to open text edit modal
                            onProfileEditClick = { isEditProfileModalOpen = true }
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }

                AnimatedVisibility(
                    visible = animateTrigger,
                    enter = fadeIn(tween(400, delayMillis = 150)) + slideInVertically(tween(400, delayMillis = 150)) { 50 }
                ) {
                    Column {
                        GearSection(
                            profileData = userData,
                            // 👇 Pass callback to open text edit modal
                            onGearEditClick = { isEditProfileModalOpen = true }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                AnimatedVisibility(
                    visible = animateTrigger,
                    enter = fadeIn(tween(400, delayMillis = 300)) + slideInVertically(tween(400, delayMillis = 300)) { 50 }
                ) {
                    Column {
                        SettingsAndLogout(
                            currentLanguage = currentLanguage,
                            currentThemeMode = currentThemeMode,
                            onLogoutClick = {
                                viewModel.clearProfile()
                                onLogoutClick()
                            },
                            onChangeLanguage = onChangeLanguage,
                            onChangeTheme = onChangeTheme,
                            viewModel = viewModel
                        )
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

// 👇 NEW: Edit Profile Dialog Composable
@Composable
fun EditProfileDialog(
    initialName: String,
    initialBlade: String,
    initialForehand: String,
    initialBackhand: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var blade by remember { mutableStateOf(initialBlade) }
    var forehand by remember { mutableStateOf(initialForehand) }
    var backhand by remember { mutableStateOf(initialBackhand) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(AppColors.SurfaceDark)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(SharedRes.string.edit_profile), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))

            // Custom styled text fields
            EditTextField(stringResource(SharedRes.string.username), name) { name = it }
            Spacer(modifier = Modifier.height(12.dp))
            EditTextField(stringResource(SharedRes.string.blade), blade) { blade = it }
            Spacer(modifier = Modifier.height(12.dp))
            EditTextField(stringResource(SharedRes.string.forehand), forehand) { forehand = it }
            Spacer(modifier = Modifier.height(12.dp))
            EditTextField(stringResource(SharedRes.string.backhand), backhand) { backhand = it }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(SharedRes.string.cancel), color = Color.Gray)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { onSave(name, blade, forehand, backhand) },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.AccentOrange)
                ) {
                    Text(stringResource(SharedRes.string.save), color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun EditTextField(label: String, value: String, onValueChange: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label.uppercase(), color = AppColors.TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = AppColors.AccentOrange,
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                cursorColor = AppColors.AccentOrange
            ),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
private fun ProfileHeader(
    profileData: ProfileState.Success,
    onPhotoEditClick: () -> Unit,
    onProfileEditClick: () -> Unit, // 👇 Added to trigger edit
    onAvatarClick: () -> Unit
) {
    val username = profileData.name ?: "Player"
    val imageUrl = profileData.imageUrl
    val avatarGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFFF4B4B), Color(0xFF9C27B0))
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clickable { onAvatarClick() }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(AppColors.SurfaceDark)
                    .border(4.dp, avatarGradient, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alignment = BiasAlignment(0f, 0f)
                    )
                } else {
                    Text(
                        text = getInitials(username),
                        color = AppColors.TextPrimary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-2).dp, y = (-2).dp)
                    .clip(CircleShape)
                    .background(AppColors.Background)
                    .padding(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .clickable { onPhotoEditClick() }
                        .background(AppColors.AccentOrange),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = "Edit Profile Picture",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 👇 Make the username row clickable to open the edit modal
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onProfileEditClick() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = username,
                color = AppColors.TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = AppColors.TextGray, modifier = Modifier.size(18.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.3f))
                    .border(1.dp, Color(0xFFFF4B4B).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "ELO ${profileData.elo}",
                    color = Color(0xFFFF4B4B),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Win Rate: ${profileData.winRate}",
                color = AppColors.TextGray,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun AvatarPreviewDialog(
    username: String,
    imageUrl: String?,
    onEditClick: () -> Unit,
    onDismissRequest: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismissRequest,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f))
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                ),
            contentAlignment = Alignment.Center
        ) {
            // --- 2. THE ALMOST FULL-SCREEN PHOTO CARD ---
            // 👇 Changed to Box so the image can fill the entire container
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .aspectRatio(1f) // Perfect square
                    .clip(RoundedCornerShape(32.dp))
                    .background(AppColors.SurfaceDark)
                    .clickable(enabled = false) {}
                    .border(2.dp, Brush.linearGradient(colors = listOf(Color(0xFFFF4B4B).copy(alpha = 0.1f), Color(0xFF9C27B0).copy(alpha = 0.1f))), RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                // 👇 NEW: Check if imageUrl exists
                if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Full Screen Profile Picture",
                        modifier = Modifier.fillMaxSize(), // Fills the rounded corner card perfectly
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // FALLBACK INITIALS
                    Text(
                        text = getInitials(username),
                        color = AppColors.TextPrimary,
                        fontSize = 80.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(32.dp) // Kept padding here for the text
                    )
                }
            }

            // --- 3. FLOATING CLOSE BUTTON (Top Right) ---
            IconButton(
                onClick = onDismissRequest,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 40.dp, end = 24.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Preview",
                    tint = Color.White
                )
            }

            // --- 4. THE EDIT PHOTO BUTTON (Bottom Center) ---
            Button(
                onClick = {
                    onEditClick()
                    onDismissRequest()
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.AccentOrange),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Photo", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AvatarFramerDialog(
    imageBytes: ByteArray, // The image they just picked
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit // Returns the Y-Bias they chose
) {
    // State to hold the slider value (-1f to 1f)
    var verticalBias by remember { mutableStateOf(0f) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(AppColors.SurfaceDark)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Frame Your Avatar", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))

            // --- THE 1:1 PREVIEW BOX ---
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .aspectRatio(1f) // Strict 1:1 Square
                    .clip(CircleShape) // Show it exactly how the profile will look
                    .border(2.dp, AppColors.AccentOrange, CircleShape)
            ) {
                AsyncImage(
                    model = imageBytes,
                    contentDescription = "Avatar Preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alignment = BiasAlignment(0f, verticalBias)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- THE SLIDER ---
            Text("Adjust Position", color = Color.Gray, fontSize = 14.sp)
            Slider(
                value = verticalBias,
                onValueChange = { verticalBias = it },
                valueRange = -1f..1f, // -1 is Top, 1 is Bottom
                colors = SliderDefaults.colors(
                    thumbColor = AppColors.AccentOrange,
                    activeTrackColor = AppColors.AccentOrange
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- BUTTONS ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color.Gray)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { onConfirm(verticalBias) },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.AccentOrange)
                ) {
                    Text("Save & Upload", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun StatsGrid() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.EmojiEvents,
                iconTint = Color(0xFFFF4B4B),
                value = "147",
                label = "Matches Played"
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.ShowChart,
                iconTint = Color(0xFF00E676),
                value = "64%",
                label = "Win Rate"
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.TrendingUp,
                iconTint = Color(0xFF00D2FF),
                value = "5W",
                label = "Current Streak"
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.WorkspacePremium,
                iconTint = Color(0xFF9C27B0),
                value = "#234",
                label = "Global Rank"
            )
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    value: String,
    label: String
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.SurfaceDark)
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = value, color = AppColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = AppColors.TextGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun GearSection(
    profileData: ProfileState.Success,
    onGearEditClick: () -> Unit // 👇 Callback for gear edit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Build, contentDescription = null, tint = AppColors.TextGray, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(SharedRes.string.my_gear), color = AppColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.weight(1f))

            // 👇 Edit Button for gear
            IconButton(onClick = onGearEditClick, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Gear", tint = AppColors.TextGray, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        GearItem(
            label = stringResource(SharedRes.string.blade).uppercase(),
            value = profileData.blade ?: "Butterfly Viscaria",
            iconContent = { Text("🏓", fontSize = 16.sp) }
        )
        Spacer(modifier = Modifier.height(12.dp))
        GearItem(
            label = stringResource(SharedRes.string.forehand).uppercase(),
            value = profileData.rubberFh ?: "Tenergy 05",
            iconContent = { Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(0xFFFF4B4B))) }
        )
        Spacer(modifier = Modifier.height(12.dp))
        GearItem(
            label = stringResource(SharedRes.string.backhand).uppercase(),
            value = profileData.rubberBh ?: "DHS Hurricane 3 Neo",
            iconContent = { Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color.Black)) }
        )
    }
}

@Composable
private fun GearItem(
    label: String,
    value: String,
    iconContent: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.SurfaceDark)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            iconContent()
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(text = label, color = AppColors.TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, color = AppColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SettingsAndLogout(
    currentLanguage: String,
    currentThemeMode: ThemeMode,
    onLogoutClick: () -> Unit,
    onChangeLanguage: (String) -> Unit,
    onChangeTheme: (ThemeMode) -> Unit,
    viewModel: ProfileViewModel
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        LanguageSelector(currentLanguage, onChangeLanguage, viewModel)

        Spacer(modifier = Modifier.height(12.dp))

        ThemeSelector(currentThemeMode, onChangeTheme)

        Spacer(modifier = Modifier.height(12.dp))

        // Settings Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(AppColors.SurfaceDark)
                .clickable { /* TODO: Navigate to settings */ }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Settings, contentDescription = null, tint = TextGray, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text("Account Settings", color = AppColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextGray)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Logout Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFFF4B4B).copy(alpha = 0.1f))
                .border(1.dp, Color(0xFFFF4B4B).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .clickable { onLogoutClick() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = Color(0xFFFF4B4B), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(SharedRes.string.logout), color = Color(0xFFFF4B4B), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LanguageSelector(
    currentLanguage: String,
    onChangeLanguage: (String) -> Unit,
    viewModel: ProfileViewModel
) {
    var expanded by remember { mutableStateOf(false) }

    // Smoothly rotates the arrow up and down
    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f)

    val displayLanguage = when (currentLanguage) {
        "hu" -> "Magyar"
        "en" -> "English"
        else -> "English"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.SurfaceDark)
            .animateContentSize() // 👈 This makes the expansion buttery smooth!
    ) {
        // --- HEADER ROW (Always Visible) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Language, contentDescription = null, tint = TextGray, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(stringResource(SharedRes.string.language), color = AppColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.weight(1f))

            Text(displayLanguage, color = TextGray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = TextGray,
                modifier = Modifier.rotate(rotation) // Applies the rotation animation
            )
        }

        // --- EXPANDED OPTIONS ---
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(expandFrom = Alignment.Top),
            exit = shrinkVertically(shrinkTowards = Alignment.Top)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clipToBounds()
                    .padding(bottom = 8.dp) // Slight padding at the bottom of the card
            ) {
                // Subtle divider line
                HorizontalDivider(
                    color = AppColors.TextPrimary.copy(alpha = 0.05f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // Option: English
                LanguageOptionRow(
                    text = "English",
                    isSelected = currentLanguage == "en",
                    onClick = {
                        viewModel.changeLanguage("en")
                        onChangeLanguage("en")
                        viewModel.fetchUserProfile()
                        expanded = false
                    }
                )

                // Option: Magyar
                LanguageOptionRow(
                    text = "Magyar",
                    isSelected = currentLanguage == "hu",
                    onClick = {
                        viewModel.changeLanguage("hu")
                        onChangeLanguage("hu")
                        viewModel.fetchUserProfile()
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun LanguageOptionRow(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {

    val textColor = if (isSelected) AppColors.AccentOrange else AppColors.TextPrimary
    val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 56.dp, vertical = 12.dp), // Indented to perfectly align with the text above
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = text, color = textColor, fontSize = 15.sp, fontWeight = fontWeight)
        Spacer(modifier = Modifier.weight(1f))

        // Add a checkmark if it is the currently selected language
        if (isSelected) {
            Icon(Icons.Default.Check, contentDescription = "Selected", tint = AppColors.AccentOrange, modifier = Modifier.size(18.dp))
        }
    }
}
@Composable
private fun ThemeSelector(
    currentThemeMode: ThemeMode,
    onChangeTheme: (ThemeMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f)

    val displayTheme = when (currentThemeMode) {
        ThemeMode.Light -> stringResource(SharedRes.string.light)
        ThemeMode.Dark -> stringResource(SharedRes.string.dark)
        ThemeMode.System -> stringResource(SharedRes.string.system_default)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.SurfaceDark)
            .animateContentSize()
    ) {
        // --- HEADER ROW ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Palette, contentDescription = null, tint = AppColors.TextGray, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(stringResource(SharedRes.string.theme), color = AppColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.weight(1f))

            Text(displayTheme, color = AppColors.TextGray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = AppColors.TextGray,
                modifier = Modifier.rotate(rotation)
            )
        }

        // --- EXPANDED OPTIONS ---
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(expandFrom = Alignment.Top),
            exit = shrinkVertically(shrinkTowards = Alignment.Top)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clipToBounds()
                    .padding(bottom = 8.dp)
            ) {
                HorizontalDivider(
                    color = AppColors.TextPrimary.copy(alpha = 0.05f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                ThemeOptionRow(stringResource(SharedRes.string.system_default), currentThemeMode == ThemeMode.System) {
                    onChangeTheme(ThemeMode.System)
//                    expanded = false
                }
                ThemeOptionRow(stringResource(SharedRes.string.light), currentThemeMode == ThemeMode.Light) {
                    onChangeTheme(ThemeMode.Light)
//                    expanded = false
                }
                ThemeOptionRow(stringResource(SharedRes.string.dark), currentThemeMode == ThemeMode.Dark) {
                    onChangeTheme(ThemeMode.Dark)
//                    expanded = false
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionRow(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val textColor = if (isSelected) AppColors.AccentOrange else AppColors.TextPrimary
    val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 8.dp), // Adjusted padding to fit the radio button
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Radio Button on the left (or you can move it to the right if you prefer!)
        RadioButton(
            selected = isSelected,
            onClick = null, // Handled by the Row's clickable modifier
            colors = RadioButtonDefaults.colors(
                selectedColor = AppColors.AccentOrange,
                unselectedColor = AppColors.TextGray
            )
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(text = text, color = textColor, fontSize = 15.sp, fontWeight = fontWeight)
    }
}