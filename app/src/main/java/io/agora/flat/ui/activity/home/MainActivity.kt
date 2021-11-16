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
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.insets.statusBarsPadding
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.R
import io.agora.flat.common.AppNavigation
import io.agora.flat.common.LeafScreen
import io.agora.flat.common.Navigator
import io.agora.flat.common.Screen
import io.agora.flat.common.login.LoginHelper
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.compose.FlatPage
import io.agora.flat.ui.compose.LocalIsPadMode
import io.agora.flat.ui.theme.FillMaxSize
import io.agora.flat.ui.theme.MaxHeight
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

        WindowCompat.setDecorFitsSystemWindows(window, false)
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
    if (viewState.loginState == LoginState.Error) {
        val context = LocalContext.current
        LaunchedEffect(true) {
            Navigator.launchLoginActivity(context)
        }
    }

    val navController = rememberNavController()

    FlatPage(statusBarColor = Color.Transparent) {
        if (viewState.loginState == LoginState.Login) {
            if (LocalIsPadMode.current) {
                MainPad(navController, viewState, onTabSelected)
            } else {
                Main(navController, viewState, onTabSelected)
            }
        }
    }
}

@Composable
internal fun Main(navController: NavHostController, viewState: MainViewState, onTabSelected: (MainTab) -> Unit) {
    Column(Modifier
        .statusBarsPadding()
        .navigationBarsPadding()) {

        AppNavigation(navController = navController, modifier = MaxWidthSpread)

        MainBottomBar(viewState.mainTab) { selectedTab ->
            val route = when (selectedTab) {
                MainTab.Home -> Screen.Home.route
                MainTab.CloudStorage -> Screen.Cloud.route
            }

            navController.navigate(route) {
                launchSingleTop = true
                restoreState = true

                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
            }
            onTabSelected(selectedTab)
        }
    }
}

@Composable
internal fun MainPad(navController: NavHostController, viewState: MainViewState, onTabSelected: (MainTab) -> Unit) {
    Row(Modifier
        .statusBarsPadding()
        .navigationBarsPadding()) {
        Box(Modifier.width(60.dp)) {
            MainPadRail(selectedTab = viewState.mainTab) { selectedTab ->
                val route = when (selectedTab) {
                    MainTab.Home -> Screen.HomeExt.route
                    MainTab.CloudStorage -> Screen.CloudExt.route
                }

                navController.navigate(route) {
                    launchSingleTop = true
                    restoreState = true

                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                }

                onTabSelected(selectedTab)
            }
        }
        Box(Modifier.width(375.dp), Alignment.Center) {
            when (viewState.mainTab) {
                MainTab.Home -> {
                    Home(
                        navController,
                        onOpenRoomCreate = {
                            navController.navigate(LeafScreen.RoomCreate.createRoute(Screen.HomeExt)) {
                                popUpTo(LeafScreen.HomeExtInit.createRoute(Screen.HomeExt))
                            }
                        },
                        onOpenRoomJoin = {
                            navController.navigate(LeafScreen.RoomJoin.createRoute(Screen.HomeExt)) {
                                popUpTo(LeafScreen.HomeExtInit.createRoute(Screen.HomeExt))
                            }
                        },
                        onOpenRoomDetail = { rUUID, pUUID ->
                            navController.navigate(LeafScreen.RoomDetail.createRoute(Screen.HomeExt,
                                rUUID,
                                pUUID)) {
                                popUpTo(LeafScreen.HomeExtInit.createRoute(Screen.HomeExt))
                            }
                        }
                    )
                }
                MainTab.CloudStorage -> CloudStorage(navController)
            }
        }
        Divider(MaxHeight.width(1.dp))
        AppNavigation(navController, startDestination = Screen.HomeExt.route, Modifier.weight(1f))
    }
}

/**
 * Main Pad Left Navigator
 */
@Composable
internal fun MainPadRail(selectedTab: MainTab, onTabSelected: (MainTab) -> Unit) {
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
private fun MainBottomBar(selectedTab: MainTab, onTabSelected: (MainTab) -> Unit) {
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
    val mainViewState = MainViewState(loginState = LoginState.Login)
    MainPage(mainViewState) { }
}

@Composable
@Preview(device = Devices.PIXEL_C)
private fun MainPagePadPreview() {
    val mainViewState = MainViewState(loginState = LoginState.Login)
    MainPage(mainViewState) {}
}
