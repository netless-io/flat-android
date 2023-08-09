package io.agora.flat.ui.compose

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.agora.flat.Constants
import io.agora.flat.R
import io.agora.flat.data.model.Country
import io.agora.flat.ui.activity.CallingCodeActivity
import io.agora.flat.ui.theme.Blue_6
import io.agora.flat.ui.theme.Blue_7
import io.agora.flat.ui.theme.Red_6
import io.agora.flat.ui.theme.isDarkTheme
import io.agora.flat.util.isValidEmail
import io.agora.flat.util.isValidPhone

// Dynamically switch between phone and email input modes
// Use the same text field component for both modes
@Composable
fun PhoneOrEmailPasswordArea(
    value: String,
    onValueChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    callingCode: String,
    onCallingCodeChange: (String) -> Unit,
) {
    var isValidEmail by remember { mutableStateOf(true) }
    var hasEmailFocused by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val callingCodeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            if (it.resultCode == Activity.RESULT_OK) {
                val country: Country = it.data!!.getParcelableExtra(Constants.IntentKey.COUNTRY)!!
                onCallingCodeChange(country.cc)
            }
        }
    )

    val isPhone = value.isValidPhone() || "" == value

    Column(Modifier.padding(horizontal = 16.dp)) {
        Row(Modifier.height(40.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isPhone) {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                val intent = Intent(context, CallingCodeActivity::class.java)
                                callingCodeLauncher.launch(intent)
                            }
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.width(8.dp))
                    FlatTextBodyOne(text = callingCode)
                    Image(painterResource(R.drawable.ic_login_arrow_down), contentDescription = "")
                }
            } else {
                Image(painterResource(R.drawable.ic_login_email), contentDescription = "")
            }
            Spacer(Modifier.width(8.dp))
            BindPhoneTextField(
                value = value,
                onValueChange = {
                    if (isPhone) {
                        onValueChange(it)
                    } else {
                        if (isValidEmail.not() && it.isValidEmail()) {
                            isValidEmail = true
                        }
                        onValueChange(it)
                    }
                },
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f),
                onFocusChanged = {
                    if (!isPhone) {
                        if (!hasEmailFocused && it.isFocused) {
                            hasEmailFocused = true
                        }
                        if (hasEmailFocused && !it.isFocused) {
                            isValidEmail = value.isValidEmail()
                        }
                    }
                },
                keyboardOptions = if (isPhone) {
                    KeyboardOptions.Default
                } else {
                    KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email)
                },
                placeholderValue = stringResource(R.string.login_phone_or_email_input_hint)
            )
        }

        if (isPhone || isValidEmail) {
            FlatDivider(thickness = 1.dp)
        } else {
            FlatDivider(color = Red_6, thickness = 1.dp)
            FlatTextBodyTwo(stringResource(R.string.login_email_invalid_tip), color = Red_6)
        }

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.ic_login_password), contentDescription = "")
            Spacer(Modifier.width(8.dp))
            PasswordTextField(
                value = password,
                onValueChange = onPasswordChange,
                modifier = Modifier
                    .height(40.dp)
                    .weight(1f),
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                placeholderValue = stringResource(R.string.login_password_input_hint),
            )

            val image = if (showPassword)
                R.drawable.ic_login_password_show
            else
                R.drawable.ic_login_password_hide

            IconButton(onClick = { showPassword = !showPassword }) {
                Icon(painterResource(image), "")
            }
        }
        FlatDivider(thickness = 1.dp)
    }
}

@Composable
fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onFocusChanged: (FocusState) -> Unit = {},
    textStyle: TextStyle? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    placeholderValue: String?,
) {
    val darkMode = isDarkTheme()
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier.onFocusChanged {
            onFocusChanged(it)
        },
        textStyle = textStyle ?: MaterialTheme.typography.body1.copy(
            color = LocalContentColor.current.copy(LocalContentAlpha.current),
        ),
        cursorBrush = SolidColor(if (darkMode) Blue_7 else Blue_6),
        singleLine = true,
        visualTransformation = visualTransformation,
        decorationBox = { innerTextField ->
            Box(
                Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty() && placeholderValue != null) {
                    PlaceholderText(placeholderValue, darkMode)
                }
                innerTextField()
            }
        }
    )
}

@Composable
@Preview
private fun PhonePasswordAreaPreview() {
    PhoneOrEmailPasswordArea(
        value = "c@c.cc",
        onValueChange = {},
        password = "",
        onPasswordChange = {},
        callingCode = "+1",
        onCallingCodeChange = {},
    )
}
