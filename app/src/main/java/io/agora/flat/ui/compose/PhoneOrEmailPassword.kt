package io.agora.flat.ui.compose

import android.app.Activity
import android.content.Intent
import android.os.CountDownTimer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.TextButton
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
import io.agora.flat.util.isValidPassword
import io.agora.flat.util.isValidPhone
import io.agora.flat.util.isValidSmsCode
import io.agora.flat.util.showPhoneMode

@Composable
fun SendCodeInput(code: String, onCodeChange: (String) -> Unit, onSendCode: () -> Unit, ready: Boolean) {
    var isValidCode by remember { mutableStateOf(true) }
    var hasCodeFocused by remember { mutableStateOf(false) }

    var remainTime by remember { mutableStateOf(0L) }
    val countDownTimer = remember {
        object : CountDownTimer(60_000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainTime = millisUntilFinished / 1000
            }

            override fun onFinish() {
                remainTime = 0
            }
        }
    }

    val sendCodeEnable = remainTime == 0L && ready
    val sendCodeText = if (remainTime == 0L) stringResource(id = R.string.login_send_sms_code) else "${remainTime}s"

    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.ic_login_sms_code), contentDescription = "")
        Spacer(Modifier.width(8.dp))
        BindPhoneTextField(
            value = code,
            onValueChange = {
                if (it.length > 6) {
                    return@BindPhoneTextField
                }
                if (isValidCode.not() && it.isValidSmsCode()) {
                    isValidCode = true
                }
                onCodeChange(it)
            },
            modifier = Modifier
                .height(48.dp)
                .weight(1f),
            onFocusChanged = {
                if (hasCodeFocused.not() && it.isFocused) {
                    hasCodeFocused = true
                }

                if (hasCodeFocused && it.isFocused.not()) {
                    isValidCode = code.isValidSmsCode()
                }
            },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            placeholderValue = stringResource(R.string.login_code_input_placeholder),
        )
        TextButton(
            enabled = sendCodeEnable,
            onClick = {
                countDownTimer.start()
                onSendCode()
            },
        ) {
            FlatTextOnButton(text = sendCodeText)
        }
    }
    if (isValidCode) {
        FlatDivider(thickness = 1.dp)
    } else {
        FlatDivider(color = Red_6, thickness = 1.dp)
        FlatTextBodyTwo(stringResource(R.string.login_code_invalid_tip), color = Red_6)
    }
}

@Composable
fun PasswordInput(
    password: String,
    onPasswordChange: (String) -> Unit,
    placeholderValue: String? = stringResource(R.string.login_password_input_hint),
    checkValid: Boolean = false,
) {
    var isValid by remember { mutableStateOf(true) }
    var showPassword by remember { mutableStateOf(false) }
    var hasFocused by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.ic_login_password), contentDescription = "")
        Spacer(Modifier.width(8.dp))
        PasswordTextField(
            value = password,
            onValueChange = onPasswordChange,
            modifier = Modifier
                .height(48.dp)
                .weight(1f),
            onFocusChanged = {
                if (!checkValid) return@PasswordTextField

                if (hasFocused.not() && it.isFocused) {
                    hasFocused = true
                }

                if (hasFocused && it.isFocused.not()) {
                    isValid = password.isValidPassword()
                }
            },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            placeholderValue = placeholderValue,
        )

        val image = if (showPassword)
            R.drawable.ic_login_password_show
        else
            R.drawable.ic_login_password_hide

        IconButton(onClick = { showPassword = !showPassword }) {
            Icon(painterResource(image), "")
        }
    }
    if (isValid) {
        FlatDivider(thickness = 1.dp)
    } else {
        FlatDivider(color = Red_6, thickness = 1.dp)
        FlatTextBodyTwo(stringResource(R.string.login_password_invalid_tip), color = Red_6)
    }
}

// Dynamically switch between phone and email input modes
// Use the same text field component for both modes
@Composable
fun PhoneOrEmailInput(
    callingCode: String,
    value: String,
    onValueChange: (String) -> Unit,
    onCallingCodeChange: (String) -> Unit,
) {
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

    val isPhone = value.showPhoneMode()

    var isValidEmail by remember { mutableStateOf(true) }
    var hasEmailFocused by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (isPhone) {
            Row(
                modifier = Modifier
                    .height(48.dp)
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
            Spacer(Modifier.width(8.dp))
        }
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
                .height(48.dp)
                .weight(1f),
            onFocusChanged = {
                // TODO handle first un focus
                if (!isPhone) {
                    if (!hasEmailFocused && it.isFocused) {
                        hasEmailFocused = true
                    }
                    if (hasEmailFocused && !it.isFocused) {
                        isValidEmail = value.isValidEmail()
                    }
                }
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
}

@Composable
fun EmailInput(
    value: String,
    onValueChange: (String) -> Unit,
) {
    var isValidEmail by remember { mutableStateOf(true) }
    var hasEmailFocused by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.ic_login_email), contentDescription = "")
        Spacer(Modifier.width(8.dp))
        BindPhoneTextField(
            value = value,
            onValueChange = {
                if (isValidEmail.not() && it.isValidEmail()) {
                    isValidEmail = true
                }
                onValueChange(it)
            },
            modifier = Modifier
                .height(48.dp)
                .weight(1f),
            onFocusChanged = {
                if (!hasEmailFocused && it.isFocused) {
                    hasEmailFocused = true
                }
                if (hasEmailFocused && !it.isFocused) {
                    isValidEmail = value.isValidEmail()
                }
            },
            placeholderValue = stringResource(R.string.login_email_input_hint)
        )
    }

    if (isValidEmail) {
        FlatDivider(thickness = 1.dp)
    } else {
        FlatDivider(color = Red_6, thickness = 1.dp)
        FlatTextBodyTwo(stringResource(R.string.login_email_invalid_tip), color = Red_6)
    }
}


@Composable
fun PhoneInput(
    phone: String,
    onPhoneChange: (String) -> Unit,
    callingCode: String,
    onCallingCodeChange: (String) -> Unit,
    enabled: Boolean = true,
) {
    var isValidPhone by remember { mutableStateOf(true) }
    var hasPhoneFocused by remember { mutableStateOf(false) }

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

    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier
                .height(48.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        val intent = Intent(context, CallingCodeActivity::class.java)
                        callingCodeLauncher.launch(intent)
                    },
                    enabled = enabled,
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(8.dp))
            FlatTextBodyOne(text = callingCode)
            Image(painterResource(R.drawable.ic_login_arrow_down), contentDescription = "")
        }
        BindPhoneTextField(
            value = phone,
            onValueChange = {
                if (isValidPhone.not() && it.isValidPhone()) {
                    isValidPhone = true
                }
                onPhoneChange(it)
            },
            modifier = Modifier
                .height(48.dp)
                .weight(1f),
            enabled = enabled,
            onFocusChanged = {
                if (hasPhoneFocused.not() && it.isFocused) {
                    hasPhoneFocused = true
                }
                if (hasPhoneFocused && it.isFocused.not()) {
                    isValidPhone = phone.isValidPhone()
                }
            },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            placeholderValue = stringResource(R.string.login_phone_input_placeholder),
        )
    }
    if (isValidPhone) {
        FlatDivider(thickness = 1.dp)
    } else {
        FlatDivider(color = Red_6, thickness = 1.dp)
        FlatTextBodyTwo(stringResource(R.string.login_phone_invalid_tip), color = Red_6)
    }
}

@Composable
fun PhoneAndCodeArea(
    phone: String,
    onPhoneChange: (String) -> Unit,
    code: String,
    onCodeChange: (String) -> Unit,
    callingCode: String,
    onCallingCodeChange: (String) -> Unit,
    onSendCode: () -> Unit,
) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        PhoneInput(
            phone = phone,
            onPhoneChange = onPhoneChange,
            callingCode = callingCode,
            onCallingCodeChange = onCallingCodeChange,
        )

        SendCodeInput(code = code, onCodeChange = onCodeChange, onSendCode = onSendCode, ready = phone.isValidPhone())
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
    Column(Modifier.padding(horizontal = 16.dp)) {
        PhoneOrEmailInput(
            callingCode = "+86",
            value = "1234567890",
            onValueChange = {},
            onCallingCodeChange = {},
        )
        Spacer(Modifier.height(8.dp))
        PasswordInput(
            password = "",
            onPasswordChange = {},
        )
        Spacer(Modifier.height(8.dp))
        EmailInput(value = "a@a", onValueChange = {})
    }
}

@Composable
@Preview
private fun PhoneAndCodeAreaPreview() {
    PhoneAndCodeArea(
        phone = "",
        onPhoneChange = {},
        code = "",
        onCodeChange = {},
        callingCode = "+1",
        onCallingCodeChange = {},
        {},
    )
}