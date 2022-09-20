package io.agora.flat.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.agora.flat.R
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
fun BindPhoneTextField(
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
fun RoomThemeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    placeholderValue: String,
) {
    var isFocused by remember { mutableStateOf(false) }
    val dividerColor = if (isFocused) MaterialTheme.colors.primary else FlatTheme.colors.divider

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier.onFocusChanged {
            isFocused = it.isFocused
        },
        textStyle = MaterialTheme.typography.h6.copy(
            color = FlatTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
        ),
        cursorBrush = SolidColor(MaterialTheme.colors.primary),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = true,
    ) { innerTextField ->
        Box(Modifier, contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 48.dp), contentAlignment = Alignment.Center
            ) {
                innerTextField()
            }
            Spacer(
                Modifier
                    .fillMaxWidth(1f)
                    .height(1.dp)
                    .background(dividerColor)
                    .align(Alignment.BottomCenter)
            )
            if (value.isEmpty()) {
                FlatTextBodyOneSecondary(placeholderValue)
            }
            if (value.isNotBlank()) {
                IconButton(onClick = { onValueChange("") }, modifier = Modifier.align(Alignment.CenterEnd)) {
                    Icon(painterResource(id = R.drawable.ic_text_filed_clear), "", tint = FlatTheme.colors.textPrimary)
                }
            }
        }
    }
}

@Composable
fun CloudDialogTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    placeholderValue: String,
) {
    var isFocused by remember { mutableStateOf(false) }
    val dividerColor = if (isFocused) MaterialTheme.colors.primary else FlatTheme.colors.divider

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier.onFocusChanged {
            isFocused = it.isFocused
        },
        textStyle = MaterialTheme.typography.h6.copy(
            color = FlatTheme.colors.textPrimary,
            textAlign = TextAlign.Start,
        ),
        cursorBrush = SolidColor(MaterialTheme.colors.primary),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = true,
    ) { innerTextField ->
        Box(Modifier, contentAlignment = Alignment.CenterStart) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(end = 48.dp), contentAlignment = Alignment.CenterStart
            ) {
                innerTextField()
            }
            Spacer(
                Modifier
                    .fillMaxWidth(1f)
                    .height(1.dp)
                    .background(dividerColor)
                    .align(Alignment.BottomCenter)
            )
            if (value.isEmpty()) {
                FlatTextBodyOneSecondary(placeholderValue)
            }
            if (value.isNotBlank()) {
                IconButton(onClick = { onValueChange("") }, modifier = Modifier.align(Alignment.CenterEnd)) {
                    Icon(
                        painterResource(id = io.agora.flat.R.drawable.ic_text_filed_clear),
                        "",
                        tint = FlatTheme.colors.textPrimary
                    )
                }
            }
        }
    }
}

@Composable
@Preview(uiMode = 0x30)
@Preview(uiMode = 0x20)
private fun FlatTextButtonPreview() {
    FlatPage {
        Column(Modifier.padding(horizontal = 16.dp)) {
            FlatNormalVerticalSpacer()
            FlatPrimaryTextField("TextField", onValueChange = {})
            FlatNormalVerticalSpacer()
            FlatPrimaryTextField("TextButton", enabled = false, onValueChange = {})
            FlatNormalVerticalSpacer()
            BindPhoneTextField("TextButton", onValueChange = {}, placeholderValue = "placeholderValue")
            FlatNormalVerticalSpacer()
        }
    }
}