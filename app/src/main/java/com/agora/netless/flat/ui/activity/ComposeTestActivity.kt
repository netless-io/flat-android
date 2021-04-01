package com.agora.netless.flat.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.agora.netless.flat.R
import com.agora.netless.flat.ui.activity.ui.theme.FlatAndroidTheme
import com.agora.netless.flat.ui.activity.ui.theme.FlatWhite
import com.agora.netless.flat.ui.compose.BackTopAppBar
import com.google.accompanist.systemuicontroller.LocalSystemUiController
import com.google.accompanist.systemuicontroller.rememberAndroidSystemUiController

class ComposeTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlatAndroidTheme {
                val controller = rememberAndroidSystemUiController()
                controller.setStatusBarColor(FlatWhite)

                CompositionLocalProvider(LocalSystemUiController provides controller) {
                    ComposeTestScreen()
                }
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
            BackTopAppBar("测试", {})
            Box(modifier = Modifier.padding(16.dp)) {
                Image(
                    painter = painterResource(id = R.drawable.header),
                    modifier = Modifier
                        .height(180.dp)
                        .clip(shape = RoundedCornerShape(12.dp))
                        .fillMaxWidth(),
                    contentScale = ContentScale.Crop,
                    contentDescription = null
                )
            }

            Text(
                modifier = Modifier.padding(16.dp),
                text = "A day wandering through the sandhills " +
                        "in Shark Fin Cove, and a few of the " +
                        "sights I saw",
                style = typography.h6,
                overflow = TextOverflow.Ellipsis
            )

            Row() {
                TextButton(
                    modifier = Modifier.weight(1f),
                    onClick = { /*TODO*/ }) {
                    Text(text = "Hello", color = Color.White)
                }
                TextButton(
                    modifier = Modifier.weight(1f),
                    onClick = { /*TODO*/ }) {
                    Text(text = "World", color = Color.White)
                }
            }
            HelloInput()
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
            modifier = Modifier.padding(bottom = 8.dp), style = MaterialTheme.typography.h5
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
        val controller = rememberAndroidSystemUiController()
        controller.setStatusBarColor(FlatWhite)

        CompositionLocalProvider(LocalSystemUiController provides controller) {
            ComposeTestScreen()
        }
    }
}