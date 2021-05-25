package io.agora.flat.ui.activity.room

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.isFocused
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.R
import io.agora.flat.common.Navigator
import io.agora.flat.ui.compose.*
import io.agora.flat.ui.viewmodel.JoinRoomAction
import io.agora.flat.ui.viewmodel.JoinRoomViewModel
import io.agora.flat.util.showToast

@AndroidEntryPoint
class JoinRoomActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel = viewModel<JoinRoomViewModel>()
            val clipboardText by viewModel.roomUUID.collectAsState()

            JoinRoomPage(clipboardText) { action ->
                when (action) {
                    JoinRoomAction.Close -> finish()
                    is JoinRoomAction.JoinRoom -> {
                        viewModel.updateRoomConfig(
                            action.roomUUID,
                            action.openVideo,
                            action.openAudio
                        )
                        Navigator.launchRoomPlayActivity(this, roomUUID = action.roomUUID)
                    }
                    JoinRoomAction.CheckClipboardText -> viewModel.checkClipboardText()
                }
            }
        }
    }
}

@Composable
private fun JoinRoomPage(clipboardText: String, actioner: (JoinRoomAction) -> Unit) {
    var copied by remember { mutableStateOf(false) }

    var uuid by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var openAudio by remember { mutableStateOf(false) }
    var openVideo by remember { mutableStateOf(false) }
    val context = LocalContext.current

    /**
     * TODO Question Twice Render
     * 1. call VM getClipboardText and change uuid locally
     * 2. hoist uuid to VM
     */
    if (clipboardText.isNotBlank()) {
        uuid = clipboardText
    }

    FlatColumnPage {
        CloseTopAppBar(
            title = stringResource(R.string.title_join_room),
            onClose = { actioner(JoinRoomAction.Close) })
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            FlatNormalVerticalSpacer()
            Text(stringResource(R.string.room_number))
            FlatSmallVerticalSpacer()
            FlatPrimaryTextField(
                value = uuid,
                onValueChange = { uuid = it },
                placeholderValue = stringResource(R.string.input_room_number_hint),
                onFocusChanged = {
                    if (it.isFocused && !copied) {
                        actioner(JoinRoomAction.CheckClipboardText)
                        copied = true
                    }
                })
            FlatNormalVerticalSpacer()
            Text(stringResource(R.string.nickname))
            FlatPrimaryTextField(
                value = nickname,
                onValueChange = { nickname = it },
                placeholderValue = stringResource(R.string.input_nickname_hint)
            )
            FlatNormalVerticalSpacer()
            Text(stringResource(R.string.join_option))
            FlatSmallVerticalSpacer()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = openAudio,
                    onCheckedChange = { openAudio = it }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.open_audio))
                Spacer(modifier = Modifier.width(40.dp))
                Checkbox(
                    checked = openVideo,
                    onCheckedChange = { openVideo = it }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.open_video))
            }
            Spacer(modifier = Modifier.height(32.dp))
            FlatPrimaryTextButton(stringResource(R.string.join_room_join)) {
                if (uuid.isNotBlank()) {
                    actioner(JoinRoomAction.JoinRoom(uuid, openVideo, openAudio))
                } else {
                    context.showToast("room uuid should not be empty")
                }
            }
        }
    }
}

@Composable
@Preview
private fun PagePreview() {
    JoinRoomPage("1234", {})
}