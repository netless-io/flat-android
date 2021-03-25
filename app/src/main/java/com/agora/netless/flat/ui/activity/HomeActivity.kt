package com.agora.netless.flat.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.agora.netless.flat.R
import com.agora.netless.flat.ui.activities.launchUserProfileActivity
import com.agora.netless.flat.ui.activity.ui.theme.FlatAndroidTheme
import com.agora.netless.flat.ui.compose.FlatTopAppBar

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlatAndroidTheme {
                Surface(color = MaterialTheme.colors.background) {
                    Greeting("Android")
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String) {
    val context = LocalContext.current

    Column {
        FlatTopAppBar(
            title = stringResource(id = R.string.title_home),
            onEndClick = { launchUserProfileActivity(context) },
            startPaint = painterResource(id = R.drawable.ic_user_profile_head),
        )
        Spacer(modifier = Modifier.weight(1f))
        FlatHomeBottomBar()
    }
}

@Composable
fun FlatHomeBottomBar() {
    BottomAppBar(elevation = 0.dp, backgroundColor = MaterialTheme.colors.primarySurface) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Button(
                elevation = ButtonDefaults.elevation(defaultElevation = 0.dp),
                onClick = { /*TODO*/ }) {}
            Image(
                painter = painterResource(R.drawable.ic_home),
                contentDescription = null
            )
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Button(
                elevation = ButtonDefaults.elevation(defaultElevation = 0.dp),
                onClick = { /*TODO*/ }) {}
            Image(
                painter = painterResource(R.drawable.ic_cloudstorage),
                contentDescription = null
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    FlatAndroidTheme {
        Greeting("Android")
    }
}