package io.agora.flat.ui.activity.dev

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.gson.Gson
import io.agora.flat.R
import io.agora.flat.data.model.CloudStorageFile
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.compose.ComposeVideoPlayer
import io.agora.flat.ui.compose.MediaPlayback
import io.agora.flat.ui.theme.FlatAndroidTheme
import io.agora.flat.ui.theme.FlatColorWhite
import kotlin.math.roundToInt

class ComposeTestActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlatAndroidTheme {
                val controller = rememberSystemUiController()
                controller.setStatusBarColor(FlatColorWhite)
                ComposeTestScreen()
            }
        }
    }
}

@Composable
fun ComposeTestScreen() {
    Surface(color = MaterialTheme.colors.background) {
        GreetingCompose()
    }
}

@Composable
fun GreetingCompose() {
    val typography = MaterialTheme.typography

    LazyColumn(Modifier.fillMaxHeight()) {
        item {
            ComposeVideoPlayerTest(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
            Box(modifier = Modifier.padding(16.dp)) {
                Image(
                    painter = painterResource(id = R.drawable.img_home_no_history),
                    modifier = Modifier
                        .height(180.dp)
                        .clip(shape = RoundedCornerShape(12.dp))
                        .fillMaxWidth(),
                    contentScale = ContentScale.Inside,
                    contentDescription = null
                )
            }

            SwipeDeleteItem()

            Text(
                modifier = Modifier.padding(16.dp),
                text = "A day wandering through the sandhills " +
                        "in Shark Fin Cove, and a few of the " +
                        "sights I saw",
                style = typography.h6,
                overflow = TextOverflow.Ellipsis
            )

            Row {
                TextButton(
                    modifier = Modifier.weight(1f),
                    onClick = { }) {
                    Text(text = "Hello", color = Color.White)
                }
                TextButton(
                    modifier = Modifier.weight(1f),
                    onClick = { }) {
                    Text(text = "World", color = Color.White)
                }
            }
            HelloInput()
        }
    }
}

@Composable
fun ComposeVideoPlayerTest(modifier: Modifier) {
    val fileStr = """
        {"convertStep":"None","createAt":1639021269495,"fileName":"1601363746034307.mp4","fileSize":1737449,"fileURL":"https://flat-storage.oss-accelerate.aliyuncs.com/cloud-storage/2021-12/09/f0073464-9aef-480c-9bca-a2117ef97fe3/f0073464-9aef-480c-9bca-a2117ef97fe3.mp4","fileUUID":"f0073464-9aef-480c-9bca-a2117ef97fe3","region":"cn-hz","taskToken":"","taskUUID":""}
    """.trimIndent()
    val file: CloudStorageFile = Gson().fromJson(fileStr, CloudStorageFile::class.java)

    var playerControl by remember {
        mutableStateOf<MediaPlayback?>(null)
    }

    var urlString by remember {
        mutableStateOf(file.fileURL)
    }

    Box(modifier, contentAlignment = Alignment.Center) {
        ComposeVideoPlayer(
            uriString = urlString,
            onPlayEvent = {},
            onPlayerControl = { playerControl = it },
            Modifier.fillMaxSize()
        )

        Row(Modifier.align(Alignment.TopCenter)) {
            Button(onClick = {
                playerControl?.playPause()
            }) {
                Text(text = "Pause & Resume")
            }
            Button(
                onClick = {
                    urlString = "https://www.rmp-streaming.com/media/big-buck-bunny-360p.mp4"
                },
            ) {
                Text(text = "Switch url")
            }
        }

    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SwipeDeleteItem(modifier: Modifier = Modifier) = BoxWithConstraints(modifier) {
    val width = constraints.maxWidth.toFloat()
    val squareSize = 72.dp

    val swipeableState = rememberSwipeableState(0)
    val sizePx = with(LocalDensity.current) { squareSize.toPx() }
    val anchors = mapOf(width to 0, (width - sizePx) to 1) // Maps anchor points (in px) to states

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .swipeable(
                state = swipeableState,
                anchors = anchors,
                thresholds = { _, _ -> FractionalThreshold(0.3f) },
                orientation = Orientation.Horizontal
            )
    ) {
        Box(modifier = Modifier
            .offset { IntOffset((swipeableState.offset.value - width).roundToInt(), 0) }
            .fillMaxWidth(width)
            .height(50.dp)
            .background(Color.LightGray)) {
            Text(text = "SSSSSSSSSHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH")
        }
        Row(Modifier.offset { IntOffset((swipeableState.offset.value).roundToInt(), 0) }) {
            Box(
                Modifier
                    .width(72.dp)
                    .fillMaxHeight()
                    .background(Color.White), Alignment.Center
            ) {
                IconButton(onClick = { }) {
                    Icon(Icons.Outlined.Delete, contentDescription = "")
                }
            }
        }
    }
}

@Composable
fun HelloInput() {
    var name by rememberSaveable { mutableStateOf("") }
    HelloContent(name = name, onNameChange = { name = it })
}

@Composable
fun HelloContent(name: String, onNameChange: (String) -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Hello, $name",
            modifier = Modifier.padding(bottom = 8.dp), style = MaterialTheme.typography.h6
        )
        OutlinedTextField(
            value = name,
            onValueChange = { onNameChange(it) },
            label = { Text("Name") }
        )
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_2)
@Composable
fun DefaultPreview2() {
    FlatAndroidTheme {
        val controller = rememberSystemUiController()
        controller.setStatusBarColor(FlatColorWhite)

        ComposeTestScreen()
    }
}