package io.agora.flat.ui.activity.room

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.ui.res.stringResource
import io.agora.flat.R
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.compose.CloseTopAppBar
import io.agora.flat.ui.compose.FlatColumnPage

class SubscribeRoomActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlatColumnPage {
                CloseTopAppBar(
                    stringResource(id = R.string.schedule_room),
                    onClose = { finish() }) {
                }
            }
        }
    }
}