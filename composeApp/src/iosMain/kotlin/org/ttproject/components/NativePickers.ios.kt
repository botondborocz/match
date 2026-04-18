package org.ttproject.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.UIKitView
import platform.UIKit.*
import platform.Foundation.*
import org.ttproject.AppColors

// Converts Compose Color to iOS UIColor
private fun Color.toUIColor(): UIColor = UIColor(
    red = this.red.toDouble(),
    green = this.green.toDouble(),
    blue = this.blue.toDouble(),
    alpha = this.alpha.toDouble()
)

@Composable
actual fun NativeDatePickerField(value: String, label: String, onDateSelected: (String) -> Unit) {
    // 👇 Prefixed with 'compose' to prevent ANY shadowing of iOS Native properties!
    val composeBgColor = AppColors.Background.toUIColor()
    val composeSurfaceColor = AppColors.SurfaceDark.toUIColor()
    val composeTextColor = AppColors.TextPrimary.toUIColor()
    val composePlaceholderColor = AppColors.TextGray.copy(alpha = 0.5f).toUIColor()
    val composeBorderColor = AppColors.TextPrimary.copy(alpha = 0.3f).toUIColor()
    val composeIconColor = AppColors.TextGray.toUIColor()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = AppColors.TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        Spacer(modifier = Modifier.height(6.dp))

        Box(modifier = Modifier.fillMaxWidth().height(56.dp)) {

            UIKitView<UIView>(
                factory = {
                    val baseView = UIView().apply {
                        backgroundColor = composeBgColor // 👈 No more shadowing errors!
                    }

                    val borderView = UIView().apply {
                        tag = 10L
                        backgroundColor = composeBorderColor
                        layer.cornerRadius = 12.0
                        translatesAutoresizingMaskIntoConstraints = false
                    }

                    baseView.addSubview(borderView)
                    borderView.leadingAnchor.constraintEqualToAnchor(baseView.leadingAnchor).active = true
                    borderView.trailingAnchor.constraintEqualToAnchor(baseView.trailingAnchor).active = true
                    borderView.topAnchor.constraintEqualToAnchor(baseView.topAnchor).active = true
                    borderView.bottomAnchor.constraintEqualToAnchor(baseView.bottomAnchor).active = true

                    val innerView = UIView().apply {
                        tag = 20L
                        backgroundColor = composeSurfaceColor
                        layer.cornerRadius = 11.0
                        translatesAutoresizingMaskIntoConstraints = false
                    }

                    borderView.addSubview(innerView)
                    innerView.leadingAnchor.constraintEqualToAnchor(borderView.leadingAnchor, constant = 1.0).active = true
                    innerView.trailingAnchor.constraintEqualToAnchor(borderView.trailingAnchor, constant = -1.0).active = true
                    innerView.topAnchor.constraintEqualToAnchor(borderView.topAnchor, constant = 1.0).active = true
                    innerView.bottomAnchor.constraintEqualToAnchor(borderView.bottomAnchor, constant = -1.0).active = true

                    val textLabel = UILabel().apply {
                        tag = 100L
                        text = value.ifBlank { "YYYY-MM-DD" }
                        textColor = if (value.isBlank()) composePlaceholderColor else composeTextColor
                        font = UIFont.systemFontOfSize(16.0)
                        translatesAutoresizingMaskIntoConstraints = false
                    }

                    val iconView = UIImageView(UIImage.systemImageNamed("calendar")).apply {
                        tag = 200L
                        tintColor = composeIconColor
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

                    val datePicker = UIDatePicker().apply {
                        datePickerMode = UIDatePickerMode.UIDatePickerModeDate
                        preferredDatePickerStyle = UIDatePickerStyle.UIDatePickerStyleCompact
                        alpha = 0.02
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

                    baseView
                },
                update = { view ->
                    view.backgroundColor = composeBgColor
                    view.viewWithTag(10L)?.backgroundColor = composeBorderColor
                    view.viewWithTag(20L)?.backgroundColor = composeSurfaceColor

                    val labelView = view.viewWithTag(100L) as? UILabel
                    labelView?.text = value.ifBlank { "YYYY-MM-DD" }
                    labelView?.textColor = if (value.isBlank()) composePlaceholderColor else composeTextColor

                    val currentIconView = view.viewWithTag(200L) as? UIImageView
                    currentIconView?.tintColor = composeIconColor
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
actual fun NativeDropdownField(value: String, label: String, options: List<String>, onOptionSelected: (String) -> Unit) {
    val composeBgColor = AppColors.Background.toUIColor()
    val composeSurfaceColor = AppColors.SurfaceDark.toUIColor()
    val composeTextColor = AppColors.TextPrimary.toUIColor()
    val composePlaceholderColor = AppColors.TextGray.copy(alpha = 0.5f).toUIColor()
    val composeBorderColor = AppColors.TextPrimary.copy(alpha = 0.3f).toUIColor()
    val composeIconColor = AppColors.TextGray.toUIColor()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = AppColors.TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        Spacer(modifier = Modifier.height(6.dp))

        Box(modifier = Modifier.fillMaxWidth().height(56.dp)) {

            UIKitView<UIView>(
                factory = {
                    val baseView = UIView().apply {
                        backgroundColor = composeBgColor
                    }

                    val borderView = UIView().apply {
                        tag = 10L
                        backgroundColor = composeBorderColor
                        layer.cornerRadius = 12.0
                        translatesAutoresizingMaskIntoConstraints = false
                    }

                    baseView.addSubview(borderView)
                    borderView.leadingAnchor.constraintEqualToAnchor(baseView.leadingAnchor).active = true
                    borderView.trailingAnchor.constraintEqualToAnchor(baseView.trailingAnchor).active = true
                    borderView.topAnchor.constraintEqualToAnchor(baseView.topAnchor).active = true
                    borderView.bottomAnchor.constraintEqualToAnchor(baseView.bottomAnchor).active = true

                    val innerView = UIView().apply {
                        tag = 20L
                        backgroundColor = composeSurfaceColor
                        layer.cornerRadius = 11.0
                        translatesAutoresizingMaskIntoConstraints = false
                    }

                    borderView.addSubview(innerView)
                    innerView.leadingAnchor.constraintEqualToAnchor(borderView.leadingAnchor, constant = 1.0).active = true
                    innerView.trailingAnchor.constraintEqualToAnchor(borderView.trailingAnchor, constant = -1.0).active = true
                    innerView.topAnchor.constraintEqualToAnchor(borderView.topAnchor, constant = 1.0).active = true
                    innerView.bottomAnchor.constraintEqualToAnchor(borderView.bottomAnchor, constant = -1.0).active = true

                    val textLabel = UILabel().apply {
                        tag = 100L
                        text = value.ifBlank { "Select Level" }
                        textColor = if (value.isBlank()) composePlaceholderColor else composeTextColor
                        font = UIFont.systemFontOfSize(16.0)
                        translatesAutoresizingMaskIntoConstraints = false
                    }

                    val iconView = UIImageView(UIImage.systemImageNamed("chevron.down")).apply {
                        tag = 200L
                        tintColor = composeIconColor
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

                    baseView
                },
                update = { view ->
                    view.backgroundColor = composeBgColor
                    view.viewWithTag(10L)?.backgroundColor = composeBorderColor
                    view.viewWithTag(20L)?.backgroundColor = composeSurfaceColor

                    val labelView = view.viewWithTag(100L) as? UILabel
                    labelView?.text = value.ifBlank { "Select Level" }
                    labelView?.textColor = if (value.isBlank()) composePlaceholderColor else composeTextColor

                    val currentIconView = view.viewWithTag(200L) as? UIImageView
                    currentIconView?.tintColor = composeIconColor
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}