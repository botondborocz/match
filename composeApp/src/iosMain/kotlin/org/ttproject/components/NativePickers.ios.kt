package org.ttproject.components

import androidx.compose.foundation.clickable
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
import platform.UIKit.*
import platform.Foundation.*
import org.ttproject.AppColors

@Composable
actual fun NativeDatePickerField(value: String, label: String, onDateSelected: (String) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = AppColors.TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = value,
            onValueChange = { onDateSelected(it) },
            placeholder = { Text("YYYY-MM-DD", color = AppColors.TextGray.copy(alpha = 0.5f)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { showPicker = true }) {
                    Icon(Icons.Default.DateRange, contentDescription = "Open Calendar", tint = AppColors.TextGray)
                }
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
    }

    LaunchedEffect(showPicker) {
        if (showPicker) {
            val rootVc = UIApplication.sharedApplication.keyWindow?.rootViewController

            // 👇 FIX: Explicitly prefixed UIAlertControllerStyle
            val alert = UIAlertController.alertControllerWithTitle(
                "Select Date",
                "\n\n\n\n\n\n\n\n\n",
                UIAlertControllerStyle.UIAlertControllerStyleActionSheet
            )

            val datePicker = UIDatePicker()
            // 👇 FIX: Explicitly prefixed UIDatePickerMode and UIDatePickerStyle
            datePicker.datePickerMode = UIDatePickerMode.UIDatePickerModeDate
            datePicker.preferredDatePickerStyle = UIDatePickerStyle.UIDatePickerStyleWheels
            datePicker.translatesAutoresizingMaskIntoConstraints = false

            alert.view.addSubview(datePicker)
            datePicker.centerXAnchor.constraintEqualToAnchor(alert.view.centerXAnchor).active = true
            datePicker.topAnchor.constraintEqualToAnchor(alert.view.topAnchor, constant = 35.0).active = true

            // 👇 FIX: Explicitly prefixed UIAlertActionStyle
            alert.addAction(UIAlertAction.actionWithTitle("Done", UIAlertActionStyle.UIAlertActionStyleDefault) {
                val formatter = NSDateFormatter()
                formatter.dateFormat = "yyyy-MM-dd"
                onDateSelected(formatter.stringFromDate(datePicker.date))
                showPicker = false
            })

            alert.addAction(UIAlertAction.actionWithTitle("Cancel", UIAlertActionStyle.UIAlertActionStyleCancel) {
                showPicker = false
            })

            rootVc?.presentViewController(alert, animated = true, completion = null)
        }
    }
}

@Composable
actual fun NativeDropdownField(value: String, label: String, options: List<String>, onOptionSelected: (String) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = AppColors.TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = value.ifBlank { "Select Level" }, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth(),
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = AppColors.TextGray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AppColors.TextPrimary, unfocusedTextColor = AppColors.TextPrimary,
                    focusedBorderColor = AppColors.AccentOrange, unfocusedBorderColor = AppColors.TextPrimary.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Box(modifier = Modifier.matchParentSize().clickable { showPicker = true })
        }
    }

    LaunchedEffect(showPicker) {
        if (showPicker) {
            val rootVc = UIApplication.sharedApplication.keyWindow?.rootViewController

            val alert = UIAlertController.alertControllerWithTitle(
                "Select Skill Level",
                null,
                UIAlertControllerStyle.UIAlertControllerStyleActionSheet
            )

            options.forEach { option ->
                alert.addAction(UIAlertAction.actionWithTitle(option, UIAlertActionStyle.UIAlertActionStyleDefault) {
                    onOptionSelected(option)
                    showPicker = false
                })
            }

            alert.addAction(UIAlertAction.actionWithTitle("Cancel", UIAlertActionStyle.UIAlertActionStyleCancel) {
                showPicker = false
            })

            rootVc?.presentViewController(alert, animated = true, completion = null)
        }
    }
}