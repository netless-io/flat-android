package io.agora.flat.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
            ),
            textStyle = MaterialTheme.typography.body1,
            singleLine = true,
            placeholder = {
                Text(placeholderValue, style = MaterialTheme.typography.body1, color = if (darkMode) Gray_7 else Gray_3)
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
@Preview
@Preview(uiMode = 0x20)
private fun FlatTextButtonPreview() {
    FlatColumnPage {
        FlatPrimaryTextField("TextField", onValueChange = {})
        FlatNormalVerticalSpacer()
        FlatPrimaryTextField("TextButton", enabled = false, onValueChange = {})
    }
}