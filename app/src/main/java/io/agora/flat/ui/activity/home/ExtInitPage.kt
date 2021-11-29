package io.agora.flat.ui.activity.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import io.agora.flat.R

@Composable
fun ExtInitPage() {
    Box(Modifier.fillMaxSize()) {
        Image(
            painterResource(R.drawable.img_pad_home_ext_empty),
            contentDescription = null,
            Modifier.align(Alignment.Center)
        )
    }
}

@Composable
@Preview
private fun ExtInitPagePreview() {
    ExtInitPage()
}