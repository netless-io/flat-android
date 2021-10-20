package io.agora.flat.ui.activity.room

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.R
import io.agora.flat.common.Navigator
import io.agora.flat.common.android.WindowFocusObserver
import io.agora.flat.common.android.rememberAndroidClipboardController
import io.agora.flat.ui.compose.*
import io.agora.flat.ui.viewmodel.JoinRoomAction
import io.agora.flat.ui.viewmodel.JoinRoomViewModel
import io.agora.flat.util.parseRoomID
import io.agora.flat.util.showToast

@AndroidEntryPoint
class JoinRoomActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel = viewModel<JoinRoomViewModel>()
            val error by viewModel.error.collectAsState()
            val roomPlayInfo by viewModel.roomPlayInfo.collectAsState()

            LaunchedEffect(error) {
                error?.message?.let { showToast(it) }
            }

            LaunchedEffect(roomPlayInfo) {
                if (roomPlayInfo != null) {
                    Navigator.launchRoomPlayActivity(this@JoinRoomActivity, roomPlayInfo!!)
                    finish()
                }
            }

            JoinRoomPage { action ->
                when (action) {
                    JoinRoomAction.Close -> finish()
                    is JoinRoomAction.JoinRoom -> {
                        viewModel.joinRoom(action.roomID, action.openVideo, action.openAudio)
                    }
                }
            }
        }
    }
}

@Composable
private fun JoinRoomPage(actioner: (JoinRoomAction) -> Unit) {
    var uuid by remember { mutableStateOf("") }
    var openAudio by remember { mutableStateOf(false) }
    var openVideo by remember { mutableStateOf(false) }

    val clipboard = rememberAndroidClipboardController()
    val context = LocalContext.current

    WindowFocusObserver { isWindowFocused ->
        if (isWindowFocused) {
            val id = clipboard.getText().toString().parseRoomID()
            if (!id.isNullOrBlank()) {
                uuid = id
            }
        }
    }

    FlatColumnPage {
        CloseTopAppBar(title = stringResource(R.string.title_join_room), onClose = { actioner(JoinRoomAction.Close) })
        Column(Modifier
            .weight(1f)
            .padding(horizontal = 16.dp)) {
            FlatNormalVerticalSpacer()
            Text(stringResource(R.string.room_id))
            FlatSmallVerticalSpacer()
            FlatPrimaryTextField(
                value = uuid,
                onValueChange = { uuid = it },
                placeholderValue = stringResource(R.string.input_room_id_hint),
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Ascii)
            )
            FlatNormalVerticalSpacer()
            Text(stringResource(R.string.join_option))
            FlatSmallVerticalSpacer()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = openAudio,
                    onCheckedChange = { openAudio = it }
                )
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.turn_on_mic))
                Spacer(Modifier.width(40.dp))
                Checkbox(
                    checked = openVideo,
                    onCheckedChange = { openVideo = it }
                )
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.turn_on_camera))
            }
            Spacer(Modifier.height(32.dp))
            FlatPrimaryTextButton(stringResource(R.string.join)) {
                if (uuid.isNotBlank()) {
                    actioner(JoinRoomAction.JoinRoom(uuid, openVideo, openAudio))
                } else {
                    context.showToast(R.string.join_room_toast_empty)
                }
            }
        }
    }
}

@Composable
@Preview
private fun PagePreview() {
    JoinRoomPage({})
}