package org.ttproject.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.ttproject.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun NativeDatePickerField(value: String, label: String, onDateSelected: (String) -> Unit) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date
                        onDateSelected(date.toString())
                    }
                    showDatePicker = false
                }) { Text("OK", color = AppColors.AccentOrange) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = AppColors.TextGray) }
            },
            colors = DatePickerDefaults.colors(containerColor = AppColors.SurfaceDark)
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    titleContentColor = AppColors.AccentOrange, headlineContentColor = Color.White,
                    weekdayContentColor = AppColors.TextGray, dayContentColor = Color.White,
                    selectedDayContainerColor = AppColors.AccentOrange, selectedDayContentColor = Color.White,
                    todayDateBorderColor = AppColors.AccentOrange, todayContentColor = AppColors.AccentOrange
                )
            )
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = AppColors.TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = value.ifBlank { "Select Date" }, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth(),
                trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = "Calendar", tint = AppColors.TextGray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AppColors.TextPrimary, unfocusedTextColor = AppColors.TextPrimary,
                    focusedBorderColor = AppColors.AccentOrange, unfocusedBorderColor = AppColors.TextPrimary.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Box(modifier = Modifier.matchParentSize().clickable { showDatePicker = true })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun NativeDropdownField(value: String, label: String, options: List<String>, onOptionSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = AppColors.TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        Spacer(modifier = Modifier.height(6.dp))

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = value.ifBlank { "Select Level" }, onValueChange = {}, readOnly = true, modifier = Modifier.menuAnchor().fillMaxWidth(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AppColors.TextPrimary, unfocusedTextColor = AppColors.TextPrimary,
                    focusedBorderColor = AppColors.AccentOrange, unfocusedBorderColor = AppColors.TextPrimary.copy(alpha = 0.3f),
                    focusedTrailingIconColor = AppColors.AccentOrange, unfocusedTrailingIconColor = AppColors.TextGray
                ),
                shape = RoundedCornerShape(12.dp)
            )

            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(AppColors.SurfaceDark)) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, color = AppColors.TextPrimary) },
                        onClick = { onOptionSelected(option); expanded = false },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}