package io.agora.flat.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.tooling.preview.Preview
import io.agora.flat.ui.theme.FlatColorBlue
import io.agora.flat.ui.theme.FlatColorBorder
import io.agora.flat.ui.theme.FlatColorGray
import io.agora.flat.ui.theme.FlatCommonTextStyle

@Composable
fun FlatPrimaryTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholderValue: String = "",
    onFocusChanged: (FocusState) -> Unit = {},
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    Box {
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
            keyboardOptions = keyboardOptions,
        )
        if (value.isNotBlank()) {
            IconButton(onClick = { onValueChange("") }, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(Icons.Outlined.Clear, "", tint = FlatColorBorder)
            }
        }

    }
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