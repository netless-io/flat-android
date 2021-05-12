package io.agora.flat.ui.compose

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.tooling.preview.Preview
import io.agora.flat.ui.activity.ui.theme.FlatColorBlue
import io.agora.flat.ui.activity.ui.theme.FlatColorBorder
import io.agora.flat.ui.activity.ui.theme.FlatColorGray
import io.agora.flat.ui.activity.ui.theme.FlatCommonTextStyle

@Composable
fun FlatPrimaryTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholderValue: String = "",
    onFocusChanged: (FocusState) -> Unit = {},
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged(onFocusChanged),
        colors = TextFieldDefaults.textFieldColors(
            focusedIndicatorColor = FlatColorBorder,
            unfocusedIndicatorColor = FlatColorBorder,
            cursorColor = FlatColorBlue,
        ),
        textStyle = FlatCommonTextStyle,
        singleLine = true,
        placeholder = {
            Text(placeholderValue, style = FlatCommonTextStyle, color = FlatColorGray)
        },
        enabled = enabled,
    )
}

@Composable
@Preview
private fun FlatTextButtonPreview() {
    FlatColumnPage {
        FlatPrimaryTextField("TextField", onValueChange = {})
        FlatNormalVerticalSpacer()
        FlatPrimaryTextField("TextButton", enabled = false, onValueChange = {})
    }
}