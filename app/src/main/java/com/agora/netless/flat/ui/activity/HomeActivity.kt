package com.agora.netless.flat.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomAppBar
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.agora.netless.flat.R
import com.agora.netless.flat.ui.activity.ui.theme.FlatBlueAlpha50
import com.agora.netless.flat.ui.activity.ui.theme.FlatTitleTextStyle
import com.agora.netless.flat.ui.compose.FlatColumnPage
import com.agora.netless.flat.ui.compose.FlatTopAppBar
import com.agora.netless.flat.ui.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeActivity : ComponentActivity() {
    private val userViewModel: UserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (userViewModel.isUserLogin()) {
            setContent {
                HomePage()
            }
        } else {
            launchLoginActivity(this)
        }
    }
}

@Composable
fun HomePage() {
    FlatColumnPage {
        FlatHomeTopBar()
        Spacer(modifier = Modifier.weight(1f))
        FlatHomeBottomBar()
    }
}

@Composable
fun FlatHomeTopBar() {
    val context = LocalContext.current

    FlatTopAppBar(
        title = {
            Text(text = stringResource(id = R.string.title_home), style = FlatTitleTextStyle)
        },
        actions = {
            IconButton(
                onClick = { launchSettingActivity(context) }) {
                Image(
                    modifier = Modifier
                        .size(24.dp, 24.dp)
                        .clip(shape = RoundedCornerShape(12.dp)),
                    painter = painterResource(id = R.drawable.header),
                    contentScale = ContentScale.Crop,
                    contentDescription = null
                )
            }
        }
    )
}

@Composable
fun FlatHomeBottomBar() {
    BottomAppBar(elevation = 0.dp, backgroundColor = FlatBlueAlpha50) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            IconButton(onClick = { /*TODO*/ }) {
                Image(
                    painter = painterResource(R.drawable.ic_home),
                    contentDescription = null
                )
            }
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            IconButton(onClick = { /*TODO*/ }) {
                Image(
                    painter = painterResource(R.drawable.ic_cloudstorage),
                    contentDescription = null
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    HomePage()
}