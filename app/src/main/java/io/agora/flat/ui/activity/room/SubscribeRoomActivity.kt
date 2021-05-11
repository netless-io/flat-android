package io.agora.flat.ui.activity.room

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.agora.flat.R
import io.agora.flat.ui.compose.CloseTopAppBar

class SubscribeRoomActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(Modifier.fillMaxSize()) {
                CloseTopAppBar(
                    stringResource(id = R.string.subscribe_room),
                    onClose = { finish() }) {
                }
            }
        }
    }
}