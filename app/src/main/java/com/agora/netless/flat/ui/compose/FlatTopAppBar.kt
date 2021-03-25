package com.agora.netless.flat.ui.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.agora.netless.flat.R

@Composable
fun FlatTopAppBar(
    title: String,
    startPaint: Painter? = null,
    onStartClick: (() -> Unit)? = null,
    onEndClick: (() -> Unit)? = null,
) {
    val typography = MaterialTheme.typography

    TopAppBar(backgroundColor = Color.Transparent, elevation = 0.dp) {
        if (startPaint != null) {
            Image(
                painter = painterResource(id = R.drawable.ic_user_profile_head),
                modifier = Modifier
                    .padding(16.dp)
                    .size(24.dp, 24.dp)
                    .clip(shape = RoundedCornerShape(12.dp))
                    .clickable(
                        onClick = onStartClick ?: {}
                    ),
                contentScale = ContentScale.Crop,
                contentDescription = null
            )
        }

        Text(
            text = title,
            color = Color(0xFF444E60),
            style = typography.h6,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
        Image(
            modifier = Modifier
                .padding(16.dp)
                .size(24.dp, 24.dp)
                .clip(shape = RoundedCornerShape(12.dp))
                .clickable(onClick = onEndClick ?: {}),
            painter = painterResource(id = R.drawable.header),
            contentScale = ContentScale.Crop,
            contentDescription = null
        )
    }
}
