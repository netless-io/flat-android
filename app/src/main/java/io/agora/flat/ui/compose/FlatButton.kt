package io.agora.flat.ui.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.agora.flat.ui.activity.ui.theme.*
import io.agora.flat.ui.theme.Shapes

@Composable
fun FlatPrimaryTextButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val colors = if (enabled)
        ButtonDefaults.textButtonColors(
            contentColor = FlatColorGray,
            backgroundColor = FlatColorBlue
        )
    else
        ButtonDefaults.textButtonColors(
            contentColor = FlatColorGray,
            backgroundColor = FlatColorGray
        )

    TextButton(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        enabled = enabled,
        colors = colors,
        shape = Shapes.small,
        onClick = onClick
    ) {
        Text(text, style = FlatCommonTextStyle, color = FlatColorWhite)
    }
}

@Composable
fun FlatSecondaryTextButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    OutlinedButton(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        enabled = enabled,
        shape = Shapes.small,
        border = BorderStroke(1.dp, FlatColorGray),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = FlatColorGray),
        onClick = onClick
    ) {
        Text(text, style = FlatCommonTextStyle)
    }
}

@Composable
@Preview
private fun FlatTextButtonPreview() {
    FlatColumnPage {
        FlatPrimaryTextButton("TextButton", onClick = {})
        FlatNormalVerticalSpacer()
        FlatPrimaryTextButton("TextButton", enabled = false, onClick = {})

        FlatLargeVerticalSpacer()

        FlatSecondaryTextButton("TextButton", onClick = {})
        FlatNormalVerticalSpacer()
        FlatSecondaryTextButton("TextButton", enabled = false, onClick = {})
    }
}