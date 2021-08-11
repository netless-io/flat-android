package io.agora.flat.ui.activity.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.agora.flat.R
import io.agora.flat.ui.compose.FlatPage
import io.agora.flat.ui.theme.FlatTitleTextStyle

@Composable
internal fun EmptyView(imgRes: Int, message: Int, modifier: Modifier = Modifier.fillMaxSize()) {
    Box(modifier, Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painter = painterResource(id = imgRes), contentDescription = null)
            Spacer(Modifier.height(4.dp))
            Text(text = stringResource(id = message), style = FlatTitleTextStyle)
        }
    }
}

@Composable
@Preview
fun previewEmptyView() {
    FlatPage {
        EmptyView(
            imgRes = R.drawable.img_home_no_room,
            message = R.string.home_no_history_room_tip,
        )
    }
}

