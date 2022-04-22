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
                onValueChange = onPhoneChange,
                modifier = Modifier
                    .height(40.dp)
                    .weight(1f),
                onFocusChanged = {
                    if (!it.hasFocus) {
                        isValidPhone = phone.isEmpty()
                    }
                },
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
                onValueChange = onCodeChange,
                modifier = Modifier
                    .height(40.dp)
                    .weight(1f),
                onFocusChanged = {
                    if (!it.hasFocus) {
                        isValidCode = code.isEmpty()
                    }
                },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                placeholderValue = "请输入验证码",
            )
            TextButton(onClick = onSendCode) {
                FlatTextButton(text = "发送验证码")
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
