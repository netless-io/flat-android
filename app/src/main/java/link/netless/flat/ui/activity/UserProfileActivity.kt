package link.netless.flat.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.systemuicontroller.LocalSystemUiController
import com.google.accompanist.systemuicontroller.rememberAndroidSystemUiController
import dagger.hilt.android.AndroidEntryPoint
import link.netless.flat.R
import link.netless.flat.ui.activity.ui.theme.FlatAndroidTheme
import link.netless.flat.ui.activity.ui.theme.FlatRed
import link.netless.flat.ui.compose.BackTopAppBar
import link.netless.flat.ui.viewmodel.UserViewModel
import link.netless.flat.util.Resource

fun launchUserProfileActivity(context: Context) {
    val intent = Intent(context, UserProfileActivity::class.java)
    context.startActivity(intent)
}

@AndroidEntryPoint
class UserProfileActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlatAndroidTheme {
                val controller = rememberAndroidSystemUiController()
                controller.setStatusBarColor(FlatRed, darkIcons = true)

                CompositionLocalProvider(LocalSystemUiController provides controller) {
                    UserProfileScreen()
                }
            }
        }
    }
}

@Composable
fun UserProfileScreen() {
    Surface(color = MaterialTheme.colors.background) {
        Column {
            BackTopAppBar(title = stringResource(id = R.string.title_user_profile), {})
            GreetingUser()
        }
    }
}

@Composable
fun GreetingUser() {
    val userViewModel: UserViewModel = viewModel()
    val value = userViewModel.userInfoResource.observeAsState().value

    if (value == null) {
        FetchResultView(onClick = {
            userViewModel.getUsers()
        })
    } else {
        when (value.status) {
            Resource.Status.LOADING -> null
            Resource.Status.SUCCESS -> FetchResultView(onClick = {
                userViewModel.getUsers()
            }, name = value.data!!.name, sex = value.data!!.sex)
            else -> null
        }
    }
}

@Composable
fun FetchResultView(
    onClick: () -> Unit,
    name: String = "Loading Name",
    sex: String = "Loading Sex"
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = name)
        Spacer(modifier = Modifier.height(32.dp))
        Text(text = sex)
        Spacer(modifier = Modifier.height(32.dp))
        TextButton(onClick = onClick) {
            Text(text = "Fetch User Info")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    FlatAndroidTheme {
        UserProfileScreen()
    }
}
