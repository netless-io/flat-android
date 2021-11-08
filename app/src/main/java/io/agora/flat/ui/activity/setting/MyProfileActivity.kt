package io.agora.flat.ui.activity.setting

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.agora.flat.R
import io.agora.flat.common.Navigator
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.compose.BackTopAppBar
import io.agora.flat.ui.compose.FlatColumnPage

class MyProfileActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlatColumnPage {
                BackTopAppBar(title = stringResource(R.string.title_my_profile), onBackPressed = { finish() })
                SettingItemList()
            }
        }
    }
}

@Composable
private fun ColumnScope.SettingItemList() {
    val context = LocalContext.current

    LazyColumn(Modifier.weight(1f)) {
        item {
            SettingItem(
                id = R.drawable.ic_user_profile_head,
                tip = stringResource(id = R.string.title_user_info),
                onClick = { Navigator.launchUserInfoActivity(context) }
            )
        }
    }
}

@Preview(showSystemUi = false)
@Composable
fun MyProfileActivityPreview() {
    FlatColumnPage {
        BackTopAppBar(stringResource(id = R.string.title_my_profile), {})
        SettingItemList()
    }
}