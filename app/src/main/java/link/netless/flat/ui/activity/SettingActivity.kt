package link.netless.flat.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NavigateNext
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import link.netless.flat.R
import link.netless.flat.ui.activity.ui.theme.FlatAndroidTheme
import link.netless.flat.ui.activity.ui.theme.FlatCommonTextStyle
import link.netless.flat.ui.activity.ui.theme.FlatCommonTipTextStyle
import link.netless.flat.ui.compose.BackTopAppBar
import link.netless.flat.ui.compose.FlatColumnPage

fun launchSettingActivity(context: Context) {
    val intent = Intent(context, SettingActivity::class.java)
    context.startActivity(intent)
}

class SettingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlatColumnPage() {
                BackTopAppBar(
                    stringResource(id = R.string.title_setting),
                    onBackPressed = { finish() })
                SettingItemList()
            }
        }
    }
}

@Composable
fun SettingItemList() {
    val context = LocalContext.current

    LazyColumn(modifier = Modifier.fillMaxHeight()) {
        item {
            SettingItem(id = R.drawable.ic_user_profile_head, "个人信息")
            Divider(Modifier.padding(start = 44.dp, end = 16.dp), thickness = 1.dp)
            SettingItem(id = R.drawable.ic_user_profile_update, "检查更新", "2.2.0")
            Divider(Modifier.padding(start = 44.dp, end = 16.dp), thickness = 1.dp)
            SettingItem(id = R.drawable.ic_user_profile_aboutus, "吐个槽")
            Divider(Modifier.padding(start = 44.dp, end = 16.dp), thickness = 1.dp)
            SettingItem(id = R.drawable.ic_user_profile_feedback, "关于我们")
            Divider(Modifier.padding(start = 44.dp, end = 16.dp), thickness = 1.dp)
            SettingItem(
                id = R.drawable.ic_user_profile_feedback,
                "Debug Tools",
                onClick = { launchDevToolsActivity(context) })
        }
    }
}

@Composable
fun SettingItem(@DrawableRes id: Int, tip: String, desc: String = "", onClick: () -> Unit = {}) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(16.dp))
        Image(painter = painterResource(id), contentDescription = null)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = tip, style = FlatCommonTextStyle)
        Spacer(modifier = Modifier.weight(1f))
        Text(text = desc, style = FlatCommonTipTextStyle)
        Spacer(modifier = Modifier.width(8.dp))
        Icon(Icons.Outlined.NavigateNext, contentDescription = null)
        Spacer(modifier = Modifier.width(16.dp))
    }
}

@Preview(showSystemUi = false)
@Composable
fun UserSettingActivityPreview() {
    FlatAndroidTheme() {
        Surface(color = MaterialTheme.colors.background) {
            Column {
                BackTopAppBar("UserSetting", {})
                SettingItemList()
            }
        }
    }
}