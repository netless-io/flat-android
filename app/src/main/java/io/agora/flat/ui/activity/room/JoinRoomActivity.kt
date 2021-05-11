package io.agora.flat.ui.activity.room

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.Checkbox
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.isFocused
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.R
import io.agora.flat.common.AndroidClipboardController
import io.agora.flat.common.Navigator
import io.agora.flat.data.AppDatabase
import io.agora.flat.data.model.RoomConfig
import io.agora.flat.ui.activity.ui.theme.FlatColorBlue
import io.agora.flat.ui.activity.ui.theme.FlatColorBorder
import io.agora.flat.ui.activity.ui.theme.FlatColorGray
import io.agora.flat.ui.activity.ui.theme.FlatCommonTextStyle
import io.agora.flat.ui.compose.*
import io.agora.flat.util.showToast
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class JoinRoomActivity : ComponentActivity() {
    private lateinit var clipboard: AndroidClipboardController

    @Inject
    lateinit var datebase: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JoinRoomPage(::providerClipboardText, datebase)
        }
        clipboard = AndroidClipboardController(this)
    }

    // TODO
    private fun providerClipboardText(): String {
        if (clipboard.getText().isNotBlank()) {
            val regex =
                """[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}""".toRegex()
            val matchEntire = regex.find(clipboard.getText())
            if (matchEntire != null) {
                return matchEntire.value
            }
        }
        return ""
    }
}

@Composable
private fun JoinRoomPage(textProvider: () -> String, database: AppDatabase) {
    var copied by remember { mutableStateOf(false) }

    var uuid by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var openVoice by remember { mutableStateOf(false) }
    var openCamera by remember { mutableStateOf(false) }
    val context = LocalContext.current

    FlatColumnPage {
        CloseTopAppBar(
            title = stringResource(id = R.string.title_join_room),
            onClose = {})
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            FlatNormalVerticalSpacer()
            Text("房间号")
            FlatSmallVerticalSpacer()
            OutlinedTextField(
                value = uuid,
                onValueChange = { uuid = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged {
                        if (it.isFocused && !copied) {
                            uuid = textProvider()
                            copied = true
                        }
                    },
                colors = TextFieldDefaults.textFieldColors(
                    focusedIndicatorColor = FlatColorBorder,
                    unfocusedIndicatorColor = FlatColorBorder,
                    cursorColor = FlatColorBlue,
                ),
                textStyle = FlatCommonTextStyle,
                singleLine = true,
                placeholder = {
                    Text("请输入房间号", style = FlatCommonTextStyle, color = FlatColorGray)
                }
            )
            FlatNormalVerticalSpacer()
            Text("匿名")
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.textFieldColors(
                    focusedIndicatorColor = FlatColorBorder,
                    unfocusedIndicatorColor = FlatColorBorder,
                    cursorColor = FlatColorBlue,
                ),
                textStyle = FlatCommonTextStyle,
                singleLine = true,
                placeholder = {
                    Text("请输入匿名", style = FlatCommonTextStyle, color = FlatColorGray)
                }
            )
            FlatNormalVerticalSpacer()
            Text("加入选项")
            FlatSmallVerticalSpacer()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = openVoice,
                    onCheckedChange = { openVoice = it }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("开启麦克风")
                Spacer(modifier = Modifier.width(40.dp))
                Checkbox(
                    checked = openCamera,
                    onCheckedChange = { openCamera = it }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("开启摄像头")
            }
            Spacer(modifier = Modifier.height(32.dp))
            FlatPrimaryTextButton(text = "加入") {
                if (uuid.isNotBlank()) {
                    GlobalScope.launch {
                        database.roomConfigDao()
                            .insertOrUpdate(RoomConfig(uuid, openVoice, openCamera))
                    }
                    Navigator.launchRoomPlayActivity(context, roomUUID = uuid)
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
    // JoinRoomPage({ "" }, null)
}