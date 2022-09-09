package io.agora.flat.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.agora.flat.ui.theme.FlatTheme
import io.agora.flat.ui.theme.isTabletMode

@Composable
fun FlatDivider(
    modifier: Modifier = Modifier,
    color: Color = FlatTheme.colors.divider,
    thickness: Dp = if (isTabletMode()) 1.dp else 0.5.dp,
    startIndent: Dp = 0.dp,
    endIndent: Dp = 0.dp,
) {
    val indentMod = if ((startIndent.value != 0f) or (endIndent.value != 0f)) {
        Modifier.padding(start = startIndent, end = endIndent)
    } else {
        Modifier
    }
    Box(
        modifier
            .then(indentMod)
            .fillMaxWidth()
            .height(thickness)
            .background(color = color)
    )
}