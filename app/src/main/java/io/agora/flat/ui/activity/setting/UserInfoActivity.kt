package io.agora.flat.ui.activity.setting

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NavigateNext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.R
import io.agora.flat.common.Navigator
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.compose.*
import io.agora.flat.ui.viewmodel.UserViewModel

@AndroidEntryPoint
class UserInfoActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlatColumnPage {
                val viewModel = viewModel(UserViewModel::class.java)
                val userInfo = viewModel.userInfo.collectAsState()

                BackTopAppBar(stringResource(R.string.title_user_info), { finish() })
                userInfo.value?.apply {
                    SettingList(name)
                }

                LifecycleHandler(
                    onResume = {
                        viewModel.refreshUser()
                    }
                )
            }
        }
    }
}

@Composable
fun SettingList(name: String) {
    val context = LocalContext.current

    LazyColumn(Modifier.fillMaxWidth()) {
        item {
            SettingItem(
                id = R.drawable.ic_user_profile_head,
                tip = stringResource(R.string.username),
                desc = name,
                onClick = { Navigator.launchEditNameActivity(context) }
            )
        }
    }
}

@Composable
private fun Item(
    tip: String,
    desc: String = "",
    onClickOrNull: (() -> Unit)? = null,
) {
    val modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)

    if (onClickOrNull != null) {
        modifier.clickable(onClick = onClickOrNull)
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Spacer(modifier = Modifier.width(16.dp))
        FlatTextBodyOne(tip)
        Spacer(modifier = Modifier.weight(1f))
        FlatTextBodyOneSecondary(desc)
        if (onClickOrNull != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Outlined.NavigateNext, contentDescription = null)
        }
        Spacer(modifier = Modifier.width(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    FlatColumnPage {
        SettingList("Name")
    }
}
