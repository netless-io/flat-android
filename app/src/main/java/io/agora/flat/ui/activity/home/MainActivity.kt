package io.agora.flat.ui.activity.home

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.R
import io.agora.flat.common.Navigator
import io.agora.flat.common.login.LoginHelper
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.activity.home.mainext.MainExtPage
import io.agora.flat.ui.compose.FlatPage
import io.agora.flat.ui.compose.LocalPadMode
import io.agora.flat.ui.theme.FillMaxSize
import io.agora.flat.ui.theme.FlatColorDivider
import io.agora.flat.ui.theme.MaxWidthSpread
import io.agora.flat.util.showToast
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull

@AndroidEntryPoint
class MainActivity : BaseComposeActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val loginHelper = LoginHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel: MainViewModel = viewModel()
            val viewState by viewModel.state.collectAsState()

            MainPage(viewState, viewModel::onMainTabSelected)
        }
        loginHelper.register()
        observerState()
    }

    private fun observerState() {
        lifecycleScope.launchWhenStarted {
            viewModel.roomPlayInfo.filterNotNull().collect {
                Navigator.launchRoomPlayActivity(this@MainActivity, it)
            }
        }
        lifecycleScope.launchWhenStarted {
            viewModel.error.filterNotNull().collect {
                showToast(it.message)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        actionLoginState()
    }

    override fun onDestroy() {
        super.onDestroy()
        loginHelper.unregister()
    }

    private fun actionLoginState() {
        if (!viewModel.isLoggedIn()) {
            Navigator.launchLoginActivity(this)
        }
    }
}

@Composable
fun MainPage(viewState: MainViewState, onTabSelected: (MainTab) -> Unit) {
    val context = LocalContext.current
    if (viewState.loginState == LoginState.Error) {
        LaunchedEffect(true) {
            Navigator.launchLoginActivity(context)
        }
    }

    val systemUiController = rememberSystemUiController()
    val useDarkIcons = MaterialTheme.colors.isLight

    SideEffect {
        systemUiController.setSystemBarsColor(
            color = Color.Transparent,
            darkIcons = useDarkIcons
        )
    }

    FlatPage {
        if (LocalPadMode.current) {
            MainContentPad(viewState, onTabSelected)
        } else {
            MainContent(viewState, onTabSelected)
        }
    }
}

@Composable
internal fun MainContent(viewState: MainViewState, onTabSelected: (MainTab) -> Unit) {
    Column {
        Box(MaxWidthSpread, Alignment.Center) {
            if (viewState.loginState == LoginState.Login) {
                when (viewState.mainTab) {
                    MainTab.Home -> Home()
                    MainTab.CloudStorage -> CloudStorage()
                }
            }
        }
        FlatHomeBottomBar(viewState.mainTab, onTabSelected)
    }
}

@Composable
internal fun MainContentPad(viewState: MainViewState, onTabSelected: (MainTab) -> Unit) {
    val navController = rememberNavController()

    Row {
        Box(Modifier.width(60.dp)) {
            MainContentPadSwitch(selectedTab = viewState.mainTab, onTabSelected = onTabSelected)
        }
        Box(Modifier
            .width(375.dp)
            .fillMaxHeight(), Alignment.Center) {
            if (viewState.loginState == LoginState.Login) {
                when (viewState.mainTab) {
                    MainTab.Home -> Home(navController)
                    MainTab.CloudStorage -> CloudStorage()
                }
            }
        }
        Spacer(
            Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(FlatColorDivider),
        )
        Box(Modifier.weight(1f)) {
            MainExtPage(navController)
        }
    }
}

@Composable
internal fun MainContentPadSwitch(selectedTab: MainTab, onTabSelected: (MainTab) -> Unit) {
    val homeResId = when (selectedTab) {
        MainTab.Home -> R.drawable.ic_home_main_selected
        MainTab.CloudStorage -> R.drawable.ic_home_main_normal
    }
    val csResId = when (selectedTab) {
        MainTab.CloudStorage -> R.drawable.ic_home_cloudstorage_selected
        MainTab.Home -> R.drawable.ic_home_cloudstorage_normal
    }
    Column(
        FillMaxSize.background(MaterialTheme.colors.background),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(100.dp))
        Box {
            IconButton(onClick = { onTabSelected(MainTab.Home) }) {
                Image(painterResource(homeResId), null)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Box {
            IconButton(onClick = { onTabSelected(MainTab.CloudStorage) }) {
                Image(painterResource(csResId), null)
            }
        }
    }
}

@Composable
private fun FlatHomeBottomBar(selectedTab: MainTab, onTabSelected: (MainTab) -> Unit) {
    val homeResId = when (selectedTab) {
        MainTab.Home -> R.drawable.ic_home_main_selected
        MainTab.CloudStorage -> R.drawable.ic_home_main_normal
    }
    val csResId = when (selectedTab) {
        MainTab.CloudStorage -> R.drawable.ic_home_cloudstorage_selected
        MainTab.Home -> R.drawable.ic_home_cloudstorage_normal
    }

    Divider()
    BottomAppBar(elevation = 0.dp, backgroundColor = MaterialTheme.colors.background) {
        Box(Modifier.weight(1f), Alignment.Center) {
            IconButton(onClick = { onTabSelected(MainTab.Home) }) {
                Image(painterResource(homeResId), null)
            }
        }
        Box(Modifier.weight(1f), Alignment.Center) {
            IconButton(onClick = { onTabSelected(MainTab.CloudStorage) }) {
                Image(painterResource(csResId), null)
            }
        }
    }
}

@Composable
@Preview
private fun MainPagePreview() {
    FlatPage {
        val mainViewState = MainViewState(loginState = LoginState.Login)
        MainPage(mainViewState) { }
    }
}

@Composable
@Preview(device = Devices.PIXEL_C)
private fun MainPagePadPreview() {
    FlatPage {
        val mainViewState = MainViewState(loginState = LoginState.Login)
        MainPage(mainViewState) {

        }
    }
}
