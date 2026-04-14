package org.ttproject.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.UIKitView
import platform.UIKit.*
import platform.Foundation.*
import org.ttproject.AppColors

@Composable
actual fun NativeDatePickerField(value: String, label: String, onDateSelected: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = AppColors.TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        Spacer(modifier = Modifier.height(6.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                placeholder = { Text("YYYY-MM-DD", color = AppColors.TextGray.copy(alpha = 0.5f)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                readOnly = true,
                trailingIcon = {
                    Icon(Icons.Default.DateRange, contentDescription = "Open Calendar", tint = AppColors.TextGray)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AppColors.TextPrimary,
                    unfocusedTextColor = AppColors.TextPrimary,
                    focusedBorderColor = AppColors.AccentOrange,
                    unfocusedBorderColor = AppColors.TextPrimary.copy(alpha = 0.3f),
                    cursorColor = AppColors.AccentOrange
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // 👇 FIX 1: Added explicit <UIDatePicker> type
            UIKitView<UIDatePicker>(
                factory = {
                    val datePicker = UIDatePicker()
                    datePicker.datePickerMode = UIDatePickerMode.UIDatePickerModeDate
                    datePicker.preferredDatePickerStyle = UIDatePickerStyle.UIDatePickerStyleCompact
                    datePicker.alpha = 0.02

                    val action = UIAction.actionWithHandler { _ ->
                        val formatter = NSDateFormatter()
                        formatter.dateFormat = "yyyy-MM-dd"
                        onDateSelected(formatter.stringFromDate(datePicker.date))
                    }

                    datePicker.addAction(action, forControlEvents = UIControlEventValueChanged)
                    datePicker
                },
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

@Composable
actual fun NativeDropdownField(value: String, label: String, options: List<String>, onOptionSelected: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = AppColors.TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        Spacer(modifier = Modifier.height(6.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = value.ifBlank { "Select Level" },
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = AppColors.TextGray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AppColors.TextPrimary, unfocusedTextColor = AppColors.TextPrimary,
                    focusedBorderColor = AppColors.AccentOrange, unfocusedBorderColor = AppColors.TextPrimary.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // 👇 FIX 2: Added explicit <UIButton> type
            UIKitView<UIButton>(
                factory = {
                    // 👇 FIX 3: Used default constructor instead of UIButtonTypeCustom enum
                    val button = UIButton()
                    button.backgroundColor = UIColor.clearColor

                    val actions = options.map { option ->
                        UIAction.actionWithTitle(
                            title = option,
                            image = null,
                            identifier = null,
                            handler = { _ -> onOptionSelected(option) }
                        )
                    }

                    val menu = UIMenu.menuWithTitle(title = "", children = actions)
                    button.showsMenuAsPrimaryAction = true
                    button.menu = menu

                    button
                },
                modifier = Modifier.matchParentSize()
            )
        }
    }
}