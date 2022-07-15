package io.agora.flat.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.agora.flat.ui.theme.*

@Composable
fun FlatPrimaryTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholderValue: String = "",
    onFocusChanged: (FocusState) -> Unit = {},
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val darkMode = isDarkTheme()
    Box {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged(onFocusChanged),
            colors = TextFieldDefaults.textFieldColors(
                focusedIndicatorColor = if (darkMode) Blue_7 else Blue_6,
                unfocusedIndicatorColor = if (darkMode) Gray_8 else Gray_6,
                cursorColor = if (darkMode) Blue_7 else Blue_6,
                backgroundColor = if (darkMode) Gray_10 else Gray_0,
            ),
            textStyle = MaterialTheme.typography.body1,
            singleLine = true,
            placeholder = {
                PlaceholderText(placeholderValue, darkMode)
            },
            enabled = enabled,
            keyboardOptions = keyboardOptions,
        )
        if (value.isNotBlank()) {
            IconButton(onClick = { onValueChange("") }, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(Icons.Outlined.Clear, "", tint = if (darkMode) Gray_8 else Gray_6)
            }
        }
    }
}

@Composable
fun FastBasicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onFocusChanged: (FocusState) -> Unit = {},
    textStyle: TextStyle? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
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
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
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
private fun PlaceholderText(placeholderValue: String, darkMode: Boolean) {
    Text(placeholderValue, style = MaterialTheme.typography.body1, color = if (darkMode) Gray_7 else Gray_3)
}

@Composable
@Preview(uiMode = 0x30)
@Preview(uiMode = 0x20)
private fun FlatTextButtonPreview() {
    FlatColumnPage {
        Column(Modifier.padding(horizontal = 16.dp)) {
            FlatNormalVerticalSpacer()
            FlatPrimaryTextField("TextField", onValueChange = {})
            FlatNormalVerticalSpacer()
            FlatPrimaryTextField("TextButton", enabled = false, onValueChange = {})
        }
    }
}