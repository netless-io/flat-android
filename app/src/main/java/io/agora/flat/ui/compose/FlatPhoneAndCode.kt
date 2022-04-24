import android.os.CountDownTimer
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.agora.flat.R
import io.agora.flat.ui.compose.*
import io.agora.flat.ui.theme.Red_6
import io.agora.flat.util.isValidPhone
import io.agora.flat.util.isValidSmsCode

@Composable
fun PhoneAndCodeArea(
    phone: String,
    onPhoneChange: (String) -> Unit,
    code: String,
    onCodeChange: (String) -> Unit,
    onSendCode: () -> Unit,
) {
    var isValidPhone by remember { mutableStateOf(true) }
    var isValidCode by remember { mutableStateOf(true) }

    var hasCodeFocused by remember { mutableStateOf(false) }
    var hasPhoneFocused by remember { mutableStateOf(false) }

    var remainTime by remember { mutableStateOf(0L) }
    val countDownTimer = remember {
        object : CountDownTimer(60_000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainTime = millisUntilFinished / 1000
            }

            override fun onFinish() {
                remainTime = 0;
            }
        }
    }

    val sendCodeEnable = remainTime == 0L
    val sendCodeText = if (remainTime == 0L) "发送验证码" else "$remainTime"

    Column(Modifier.padding(horizontal = 16.dp)) {
        FlatTextCaption(text = "手机号")
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FlatTextBodyOne(text = "+86")
            Image(painterResource(R.drawable.ic_login_arrow_down), contentDescription = "")
            Spacer(modifier = Modifier.width(8.dp))
            FastBasicTextField(
                value = phone,
                onValueChange = {
                    if (isValidPhone.not() && it.isValidPhone()) {
                        isValidPhone = true
                    }
                    onPhoneChange(it)
                },
                modifier = Modifier
                    .height(40.dp)
                    .weight(1f),
                onFocusChanged = {
                    if (hasPhoneFocused.not() && it.isFocused) {
                        hasPhoneFocused = true
                    }
                    if (hasPhoneFocused && it.isFocused.not()) {
                        isValidPhone = phone.isValidPhone()
                    }
                },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                placeholderValue = "请输入手机号",
            )
        }
        if (isValidPhone) {
            FlatDivider(thickness = 1.dp)
        } else {
            FlatDivider(color = Red_6, thickness = 1.dp)
            FlatTextBodyTwo(text = "请填写正确手机号", color = Red_6)
        }
        Spacer(modifier = Modifier.height(16.dp))
        FlatTextCaption(text = "验证码")
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.ic_login_sms_code), contentDescription = "")
            Spacer(modifier = Modifier.width(8.dp))
            FastBasicTextField(
                value = code,
                onValueChange = {
                    if (isValidCode.not() && it.isValidSmsCode()) {
                        isValidCode = true
                    }
                    onCodeChange(it)
                },
                modifier = Modifier
                    .height(40.dp)
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
                placeholderValue = "请输入验证码",
            )
            TextButton(
                enabled = sendCodeEnable,
                onClick = {
                    countDownTimer.start()
                    onSendCode()
                },
            ) {
                FlatTextButton(text = sendCodeText)
            }
        }
        if (isValidCode) {
            FlatDivider(thickness = 1.dp)
        } else {
            FlatDivider(color = Red_6, thickness = 1.dp)
            FlatTextBodyTwo(text = "请填写正确验证码", color = Red_6)
        }
    }
}

@Composable
@Preview
private fun PhoneAndCodeAreaPreview() {
    PhoneAndCodeArea(
        "",
        {},
        "",
        {},
        {}
    )
}
