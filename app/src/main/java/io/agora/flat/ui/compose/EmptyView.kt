package io.agora.flat.ui.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.agora.flat.R
import io.agora.flat.ui.theme.FlatTheme

@Composable
internal fun EmptyView(modifier: Modifier = Modifier, imgRes: Int, message: Int) {
    val textColor = FlatTheme.colors.textPrimary
    
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box {
                Image(
                    painterResource(id = imgRes),
                    contentDescription = null,
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
                imgRes = R.drawable.img_room_list_empty_light,
                message = R.string.home_no_history_room_tip,
            )
        }
    }
}