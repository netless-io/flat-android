package io.agora.flat.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.ui.activity.ui.theme.FlatCommonTextStyle
import io.agora.flat.ui.compose.BackTopAppBar
import io.agora.flat.ui.compose.FlatColumnPage
import io.agora.flat.ui.viewmodel.UserViewModel

@AndroidEntryPoint
class DevToolsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FlatColumnPage {
                BackTopAppBar(
                    title = "DevTools",
                    onBackPressed = { finish() }
                )

                LazyColumn(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    item {
                        UserLoginFlag()
                    }
                }
            }
        }
    }
}

@Composable
fun UserLoginFlag() {
    val userViewModel: UserViewModel = viewModel()
    val loggedInData = userViewModel.loggedInData.observeAsState()

    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = "设置User", style = FlatCommonTextStyle)
        Spacer(modifier = Modifier.weight(1f))
        Switch(
            checked = loggedInData.value ?: false,
            enabled = loggedInData.value ?: true,
            onCheckedChange = { userViewModel.logout() })
        Spacer(modifier = Modifier.width(16.dp))
    }
}


@Preview
@Composable
fun PreviewDevTools() {
    FlatColumnPage {
        BackTopAppBar(title = "DevTools", {})

        LazyColumn(
            Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            item {
                UserLoginFlag()
            }
        }
    }
}