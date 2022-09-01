package io.agora.flat.ui.compose

import androidx.annotation.DrawableRes
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import io.agora.flat.ui.theme.Gray_3
import io.agora.flat.ui.theme.Gray_6
import io.agora.flat.ui.theme.isDarkTheme


@Composable
fun FlatIcon(
    @DrawableRes id: Int,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    tint: Color = if (isDarkTheme()) Gray_3 else Gray_6,
) {
    Icon(
        painterResource(id = id),
        contentDescription = contentDescription,
        tint = tint
    )
}