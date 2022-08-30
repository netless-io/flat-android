package io.agora.flat.ui.activity.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.agora.flat.R
import io.agora.flat.ui.compose.FlatPage
import io.agora.flat.ui.compose.FlatTextBodyOne
import io.agora.flat.ui.compose.FlatTextBodyTwo
import io.agora.flat.ui.theme.Blue_0
import io.agora.flat.ui.theme.Blue_10
import io.agora.flat.ui.theme.FlatTheme
import io.agora.flat.ui.theme.isDarkTheme

@Composable
internal fun LastEmptyView(imgRes: Int, message: Int, modifier: Modifier = Modifier.fillMaxSize()) {
    Box(modifier, Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painter = painterResource(id = imgRes), contentDescription = null)
            Spacer(Modifier.height(4.dp))
            FlatTextBodyOne(text = stringResource(id = message))
        }
    }
}

@Composable
internal fun EmptyView(modifier: Modifier = Modifier, imgRes: Int, message: Int) {
    val bgColor = if (isDarkTheme()) Blue_10 else Blue_0
    val imgColor = MaterialTheme.colors.primary
    val textColor = FlatTheme.colors.textSecondary
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box {
                Image(
                    painterResource(R.drawable.img_empty_bg),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(bgColor)
                )
                Image(
                    painterResource(id = imgRes),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(imgColor)
                )
            }
            FlatTextBodyTwo(stringResource(message), color = textColor)
        }
    }
}

@Composable
@Preview(widthDp = 400, heightDp = 800, uiMode = 0x10, locale = "zh")
@Preview(widthDp = 400, heightDp = 800, uiMode = 0x20)
private fun PreviewEmptyView() {
    FlatPage {
        Box {
            EmptyView(
                modifier = Modifier
                    .width(200.dp)
                    .align(Alignment.Center),
                imgRes = R.drawable.img_room_list_empty,
                message = R.string.home_no_history_room_tip,
            )
        }
    }
}