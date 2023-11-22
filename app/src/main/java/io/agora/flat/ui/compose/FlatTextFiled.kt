package io.agora.flat.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.agora.flat.R
import io.agora.flat.ui.theme.Blue_6
import io.agora.flat.ui.theme.Blue_7
import io.agora.flat.ui.theme.FlatTheme
import io.agora.flat.ui.theme.Gray_0
import io.agora.flat.ui.theme.Gray_10
import io.agora.flat.ui.theme.Gray_3
import io.agora.flat.ui.theme.Gray_6
import io.agora.flat.ui.theme.Gray_7
import io.agora.flat.ui.theme.Gray_8
import io.agora.flat.ui.theme.isDarkTheme

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
fun FlatBasicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onFocusChanged: (FocusState) -> Unit = {},
    textStyle: TextStyle? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    placeholderValue: String?,
) {
    val darkMode = isDarkTheme()
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.onFocusChanged {
            onFocusChanged(it)
        },
        enabled = enabled,
        textStyle = textStyle ?: MaterialTheme.typography.body1.copy(
            color = LocalContentColor.current.copy(LocalContentAlpha.current),
        ),
        cursorBrush = SolidColor(if (darkMode) Blue_7 else Blue_6),
        singleLine = true,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
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

                if (value.isNotBlank() && enabled) {
                    IconButton(onClick = { onValueChange("") }, modifier = Modifier.align(Alignment.CenterEnd)) {
                        Icon(
                            painterResource(id = R.drawable.ic_text_filed_clear),
                            "",
                            tint = FlatTheme.colors.textSecondary
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun PlaceholderText(placeholderValue: String, darkMode: Boolean) {
    Text(placeholderValue, style = MaterialTheme.typography.body1, color = if (darkMode) Gray_7 else Gray_3)
}

@Composable
fun JoinRoomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholderValue: String,
    onExtendButtonClick: (() -> Unit)? = null,
) {
    var isFocused by remember { mutableStateOf(false) }
    val dividerColor = if (isFocused) MaterialTheme.colors.primary else FlatTheme.colors.divider

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier.onFocusChanged { isFocused = it.isFocused },
        textStyle = TextStyle(
            color = FlatTheme.colors.textPrimary,
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp,
            letterSpacing = 0.15.sp,
            textAlign = TextAlign.Center
        ),
        cursorBrush = SolidColor(MaterialTheme.colors.primary),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        keyboardActions = KeyboardActions.Default,
        singleLine = true,
        visualTransformation = RoomNumInputVisualTransformation()
    ) { innerTextField ->
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp), contentAlignment = Alignment.Center
            ) {
                innerTextField()
            }
            FlatDivider(Modifier.align(Alignment.BottomCenter), color = dividerColor)
            if (value.isEmpty()) {
                FlatTextBodyOneSecondary(placeholderValue)
            }
            Row(modifier = Modifier.align(Alignment.CenterEnd)) {
                if (isFocused && value.isNotBlank()) {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(
                            painterResource(id = R.drawable.ic_text_filed_clear),
                            "",
                            tint = FlatTheme.colors.textPrimary
                        )
                    }
                }

                if (onExtendButtonClick != null) {
                    IconButton(onClick = onExtendButtonClick) {
                        Icon(painterResource(id = R.drawable.ic_record_arrow_down), "", tint = FlatTheme.colors.textPrimary)
                    }
                }
            }
        }
    }
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
        textStyle = TextStyle(
            color = FlatTheme.colors.textPrimary,
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp,
            letterSpacing = 0.15.sp,
            textAlign = TextAlign.Center
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
            FlatDivider(Modifier.align(Alignment.BottomCenter), color = dividerColor)
            if (value.isEmpty()) {
                FlatTextBodyOneSecondary(placeholderValue)
            }
            if (value.isNotBlank()) {
                IconButton(onClick = { onValueChange("") }, modifier = Modifier.align(Alignment.CenterEnd)) {
                    Icon(
                        painterResource(id = R.drawable.ic_text_filed_clear),
                        "",
                        tint = FlatTheme.colors.textPrimary
                    )
                }
            }
        }
    }
}

class RoomNumInputVisualTransformation : VisualTransformation {
    private val offsetMapping = object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int {
            if (offset <= 4) return offset
            if (offset <= 7) return offset + 1
            if (offset <= 11) return offset + 2
            return offset
        }

        override fun transformedToOriginal(offset: Int): Int {
            if (offset <= 4) return offset
            if (offset <= 8) return offset - 1
            if (offset <= 13) return offset - 2
            return offset
        }
    }

    override fun filter(text: AnnotatedString): TransformedText {
        val input = text.toString()
        val transformedText = when {
            input.length > 11 -> input
            input.length > 7 -> "${input.substring(0, 4)} ${input.substring(4, 7)} ${input.substring(7)}"
            input.length > 4 -> "${input.substring(0, 4)} ${input.substring(4)}"
            else -> input
        }
        return TransformedText(
            AnnotatedString(transformedText),
            offsetMapping
        )
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
            FlatBasicTextField("TextButton", onValueChange = {}, placeholderValue = "placeholderValue")
            FlatNormalVerticalSpacer()

            JoinRoomTextField(
                value = "12345678901",
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                placeholderValue = "placeholderValue"
            )
        }
    }
}