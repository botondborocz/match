package org.ttproject.components

import androidx.compose.foundation.layout.*
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

        Box(modifier = Modifier.fillMaxWidth().height(56.dp)) {

            UIKitView<UIView>(
                factory = {
                    // 1. The "Border" View (Slightly larger, white with 30% alpha)
                    val container = UIView().apply {
                        backgroundColor = UIColor.whiteColor.colorWithAlphaComponent(0.3)
                        layer.cornerRadius = 12.0
                    }

                    // 2. The "Inner" View (Dark, 1 pixel smaller on all sides)
                    val innerView = UIView().apply {
                        backgroundColor = UIColor(red = 30.0/255.0, green = 37.0/255.0, blue = 50.0/255.0, alpha = 1.0)
                        layer.cornerRadius = 11.0
                        translatesAutoresizingMaskIntoConstraints = false
                    }

                    // Add innerView to container and force a 1px gap to create the "border"
                    container.addSubview(innerView)
                    innerView.leadingAnchor.constraintEqualToAnchor(container.leadingAnchor, constant = 1.0).active = true
                    innerView.trailingAnchor.constraintEqualToAnchor(container.trailingAnchor, constant = -1.0).active = true
                    innerView.topAnchor.constraintEqualToAnchor(container.topAnchor, constant = 1.0).active = true
                    innerView.bottomAnchor.constraintEqualToAnchor(container.bottomAnchor, constant = -1.0).active = true

                    // 3. Add the text natively
                    val textLabel = UILabel().apply {
                        tag = 100L
                        text = value.ifBlank { "YYYY-MM-DD" }
                        textColor = if (value.isBlank()) UIColor.grayColor else UIColor.whiteColor
                        font = UIFont.systemFontOfSize(16.0)
                        translatesAutoresizingMaskIntoConstraints = false
                    }

                    // 4. Add the icon natively
                    val iconView = UIImageView(UIImage.systemImageNamed("calendar")).apply {
                        tintColor = UIColor.grayColor
                        translatesAutoresizingMaskIntoConstraints = false
                    }

                    innerView.addSubview(textLabel)
                    innerView.addSubview(iconView)

                    textLabel.leadingAnchor.constraintEqualToAnchor(innerView.leadingAnchor, constant = 15.0).active = true
                    textLabel.centerYAnchor.constraintEqualToAnchor(innerView.centerYAnchor).active = true

                    iconView.trailingAnchor.constraintEqualToAnchor(innerView.trailingAnchor, constant = -15.0).active = true
                    iconView.centerYAnchor.constraintEqualToAnchor(innerView.centerYAnchor).active = true
                    iconView.widthAnchor.constraintEqualToConstant(20.0).active = true
                    iconView.heightAnchor.constraintEqualToConstant(20.0).active = true

                    // 5. Add the actual iOS Compact Date Picker
                    val datePicker = UIDatePicker().apply {
                        datePickerMode = UIDatePickerMode.UIDatePickerModeDate
                        preferredDatePickerStyle = UIDatePickerStyle.UIDatePickerStyleCompact
                        alpha = 0.02 // Invisible, but intercepts physical touches!
                        translatesAutoresizingMaskIntoConstraints = false
                    }

                    val action = UIAction.actionWithHandler { _ ->
                        val formatter = NSDateFormatter()
                        formatter.dateFormat = "yyyy-MM-dd"
                        onDateSelected(formatter.stringFromDate(datePicker.date))
                    }
                    datePicker.addAction(action, forControlEvents = UIControlEventValueChanged)

                    innerView.addSubview(datePicker)
                    datePicker.leadingAnchor.constraintEqualToAnchor(innerView.leadingAnchor).active = true
                    datePicker.trailingAnchor.constraintEqualToAnchor(innerView.trailingAnchor).active = true
                    datePicker.topAnchor.constraintEqualToAnchor(innerView.topAnchor).active = true
                    datePicker.bottomAnchor.constraintEqualToAnchor(innerView.bottomAnchor).active = true

                    container // Return the outer border view
                },
                update = { view ->
                    // The label is now inside the innerView, so we search the subviews
                    val inner = view.subviews.firstOrNull() as? UIView
                    val labelView = inner?.viewWithTag(100L) as? UILabel
                    labelView?.text = value.ifBlank { "YYYY-MM-DD" }
                    labelView?.textColor = if (value.isBlank()) UIColor.grayColor else UIColor.whiteColor
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
actual fun NativeDropdownField(value: String, label: String, options: List<String>, onOptionSelected: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = AppColors.TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        Spacer(modifier = Modifier.height(6.dp))

        Box(modifier = Modifier.fillMaxWidth().height(56.dp)) {

            UIKitView<UIView>(
                factory = {
                    // 1. The "Border" View
                    val container = UIView().apply {
                        backgroundColor = UIColor.whiteColor.colorWithAlphaComponent(0.3)
                        layer.cornerRadius = 12.0
                    }

                    // 2. The "Inner" View
                    val innerView = UIView().apply {
                        backgroundColor = UIColor(red = 30.0/255.0, green = 37.0/255.0, blue = 50.0/255.0, alpha = 1.0)
                        layer.cornerRadius = 11.0
                        translatesAutoresizingMaskIntoConstraints = false
                    }

                    container.addSubview(innerView)
                    innerView.leadingAnchor.constraintEqualToAnchor(container.leadingAnchor, constant = 1.0).active = true
                    innerView.trailingAnchor.constraintEqualToAnchor(container.trailingAnchor, constant = -1.0).active = true
                    innerView.topAnchor.constraintEqualToAnchor(container.topAnchor, constant = 1.0).active = true
                    innerView.bottomAnchor.constraintEqualToAnchor(container.bottomAnchor, constant = -1.0).active = true

                    // 3. Native text label
                    val textLabel = UILabel().apply {
                        tag = 100L
                        text = value.ifBlank { "Select Level" }
                        textColor = if (value.isBlank()) UIColor.grayColor else UIColor.whiteColor
                        font = UIFont.systemFontOfSize(16.0)
                        translatesAutoresizingMaskIntoConstraints = false
                    }

                    // 4. Native chevron icon
                    val iconView = UIImageView(UIImage.systemImageNamed("chevron.down")).apply {
                        tintColor = UIColor.grayColor
                        translatesAutoresizingMaskIntoConstraints = false
                    }

                    innerView.addSubview(textLabel)
                    innerView.addSubview(iconView)

                    textLabel.leadingAnchor.constraintEqualToAnchor(innerView.leadingAnchor, constant = 15.0).active = true
                    textLabel.centerYAnchor.constraintEqualToAnchor(innerView.centerYAnchor).active = true

                    iconView.trailingAnchor.constraintEqualToAnchor(innerView.trailingAnchor, constant = -15.0).active = true
                    iconView.centerYAnchor.constraintEqualToAnchor(innerView.centerYAnchor).active = true
                    iconView.widthAnchor.constraintEqualToConstant(20.0).active = true
                    iconView.heightAnchor.constraintEqualToConstant(20.0).active = true

                    // 5. The Native iOS Menu Button Overlay
                    val button = UIButton().apply {
                        showsMenuAsPrimaryAction = true
                        backgroundColor = UIColor.clearColor
                        translatesAutoresizingMaskIntoConstraints = false

                        val actions = options.map { option ->
                            UIAction.actionWithTitle(
                                title = option,
                                image = null,
                                identifier = null,
                                handler = { _ -> onOptionSelected(option) }
                            )
                        }
                        menu = UIMenu.menuWithTitle(title = "", children = actions)
                    }

                    innerView.addSubview(button)
                    button.leadingAnchor.constraintEqualToAnchor(innerView.leadingAnchor).active = true
                    button.trailingAnchor.constraintEqualToAnchor(innerView.trailingAnchor).active = true
                    button.topAnchor.constraintEqualToAnchor(innerView.topAnchor).active = true
                    button.bottomAnchor.constraintEqualToAnchor(innerView.bottomAnchor).active = true

                    container
                },
                update = { view ->
                    val inner = view.subviews.firstOrNull() as? UIView
                    val labelView = inner?.viewWithTag(100L) as? UILabel
                    labelView?.text = value.ifBlank { "Select Level" }
                    labelView?.textColor = if (value.isBlank()) UIColor.grayColor else UIColor.whiteColor
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}