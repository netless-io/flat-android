package io.agora.flat.ui.activity.room

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.res.stringResource
import io.agora.flat.R
import io.agora.flat.ui.compose.CloseTopAppBar
import io.agora.flat.ui.compose.FlatColumnPage

class SubscribeRoomActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlatColumnPage {
                CloseTopAppBar(
                    stringResource(id = R.string.subscribe_room),
                    onClose = { finish() }) {
                }
            }
        }
    }
}