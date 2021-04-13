package link.netless.flat.ui.compose

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import link.netless.flat.ui.activity.ui.theme.*

@Composable
fun FlatPrimaryTextButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val colors = if (enabled) {
        ButtonDefaults.textButtonColors(backgroundColor = FlatColorBlue)
    } else {
        ButtonDefaults.textButtonColors(backgroundColor = FlatColorGray)
    }
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
@Preview
private fun FlatPrimaryTextButtonPreview() {
    FlatColumnPage {
        FlatPrimaryTextButton("TextButton", onClick = {})
        FlatNormalVerticalSpacer()
        FlatPrimaryTextButton("TextButton", enabled = false, onClick = {})
    }
}