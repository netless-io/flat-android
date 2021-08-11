package io.agora.flat.ui.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.agora.flat.R
import io.agora.flat.ui.theme.*

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
fun FlatHighlightTextButton(
    text: String,
    icon: Int = 0,
    color: Color = FlatColorRed,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    OutlinedButton(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        enabled = enabled,
        shape = Shapes.small,
        border = BorderStroke(1.dp, color),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        onClick = onClick
    ) {
        if (icon != 0) {
            Icon(painterResource(icon), contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(text, style = FlatCommonTextStyle, color = color)
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

        FlatNormalVerticalSpacer()
        FlatHighlightTextButton("TextButton", icon = R.drawable.ic_login_out, enabled = true, onClick = {})
    }
}