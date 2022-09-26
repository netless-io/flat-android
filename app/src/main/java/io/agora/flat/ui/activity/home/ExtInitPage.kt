package io.agora.flat.ui.activity.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import io.agora.flat.R
import io.agora.flat.ui.theme.Gray_1
import io.agora.flat.ui.theme.Gray_8
import io.agora.flat.ui.theme.isDarkTheme

@Composable
fun ExtInitPage() {
    val color = if (isDarkTheme()) Gray_8 else Gray_1

    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.img_pad_home_ext_empty),
            contentDescription = null,
            modifier = Modifier.align(Alignment.Center),
            colorFilter = ColorFilter.tint(color),
        )
    }
}

@Composable
@Preview
private fun ExtInitPagePreview() {
    ExtInitPage()
}