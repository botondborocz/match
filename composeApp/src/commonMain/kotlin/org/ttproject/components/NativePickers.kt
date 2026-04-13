package org.ttproject.components

import androidx.compose.runtime.Composable

@Composable
expect fun NativeDatePickerField(
    value: String,
    label: String,
    onDateSelected: (String) -> Unit
)

@Composable
expect fun NativeDropdownField(
    value: String,
    label: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit
)