package link.netless.flat.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import link.netless.flat.ui.activity.ui.theme.FlatTitleTextStyle
import link.netless.flat.ui.compose.BackTopAppBar
import link.netless.flat.ui.compose.FlatColumnPage
import link.netless.flat.ui.viewmodel.UserViewModel
import link.netless.flat.common.Navigator
import link.netless.flat.util.Resource


@AndroidEntryPoint
class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LoginPage {
                finish()
            }
        }
    }
}

@Composable
private fun LoginPage(onBackPressed: () -> Unit) {
    val userViewModel: UserViewModel = viewModel()
    val loginState = userViewModel.loginResource.observeAsState()

    if (loginState.value?.status == Resource.Status.SUCCESS) {
        Navigator.launchHomeActivity(context = LocalContext.current)
    } else {
        FlatColumnPage {
            BackTopAppBar(title = "Login", onBackPressed)
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Button(onClick = {
                    userViewModel.login()
                }) {
                    Text(style = FlatTitleTextStyle, text = "Tag Login")
                }
            }
        }
    }
}