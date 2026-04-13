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

    // Renders the exact same Compose input field
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
            // 👇 Triggers the native iOS popup!
            Box(modifier = Modifier.matchParentSize().clickable { showPicker = true })
        }
    }

    LaunchedEffect(showPicker) {
        if (showPicker) {
            val rootVc = UIApplication.sharedApplication.keyWindow?.rootViewController

            // Create an Action Sheet with enough space for the date picker wheel
            val alert = UIAlertController.alertControllerWithTitle("Select Date", "\n\n\n\n\n\n\n\n\n", UIAlertControllerStyleActionSheet)

            val datePicker = UIDatePicker()
            datePicker.datePickerMode = UIDatePickerModeDate
            datePicker.preferredDatePickerStyle = UIDatePickerStyleWheels
            datePicker.translatesAutoresizingMaskIntoConstraints = false

            // Center the picker inside the Action Sheet
            alert.view.addSubview(datePicker)
            datePicker.centerXAnchor.constraintEqualToAnchor(alert.view.centerXAnchor).active = true
            datePicker.topAnchor.constraintEqualToAnchor(alert.view.topAnchor, constant = 35.0).active = true

            alert.addAction(UIAlertAction.actionWithTitle("Done", UIAlertActionStyleDefault) {
                val formatter = NSDateFormatter()
                formatter.dateFormat = "yyyy-MM-dd"
                onDateSelected(formatter.stringFromDate(datePicker.date))
                showPicker = false
            })

            alert.addAction(UIAlertAction.actionWithTitle("Cancel", UIAlertActionStyleCancel) {
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

            // Native iOS Bottom Action Sheet for Dropdowns
            val alert = UIAlertController.alertControllerWithTitle("Select Skill Level", null, UIAlertControllerStyleActionSheet)

            options.forEach { option ->
                alert.addAction(UIAlertAction.actionWithTitle(option, UIAlertActionStyleDefault) {
                    onOptionSelected(option)
                    showPicker = false
                })
            }

            alert.addAction(UIAlertAction.actionWithTitle("Cancel", UIAlertActionStyleCancel) {
                showPicker = false
            })

            rootVc?.presentViewController(alert, animated = true, completion = null)
        }
    }
}