package com.agora.netless.flat.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.agora.netless.flat.R
import com.agora.netless.flat.ui.activity.ui.theme.FlatAndroidTheme

class ComposeTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlatAndroidTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    GreetingCompose()
                }
            }
        }
    }
}

@Composable
fun GreetingCompose() {
    val typography = MaterialTheme.typography

    Column() {
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
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview2() {
    FlatAndroidTheme {
        GreetingCompose()
    }
}