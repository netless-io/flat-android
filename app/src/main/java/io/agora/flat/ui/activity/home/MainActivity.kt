package io.agora.flat.ui.activity.home

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.BottomAppBar
import androidx.compose.material.Divider
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.R
import io.agora.flat.common.*
import io.agora.flat.common.login.LoginHelper
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.compose.FlatDivider
import io.agora.flat.ui.compose.FlatPage
import io.agora.flat.ui.compose.GlobalAgreementDialog
import io.agora.flat.ui.compose.LifecycleHandler
import io.agora.flat.ui.theme.FillMaxSize
import io.agora.flat.ui.theme.MaxHeight
import io.agora.flat.ui.theme.isTabletMode
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
            val viewModel: MainViewModel by viewModels()
            val viewState by viewModel.state.collectAsState()

            if (viewState.protocolAgreed) {
                MainScreen(viewState)
                LifecycleHandler(
                    onResume = {
                        if (!viewModel.isLoggedIn()) {
                            Navigator.launchLoginActivity(this)
                        }
                    },
                )
            } else {
                GlobalAgreementDialog(
                    onAgree = { viewModel.agreeProtocol() },
                    onRefuse = { finish() },
                )
            }
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

    override fun onDestroy() {
        super.onDestroy()
        loginHelper.unregister()
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainScreen(viewState: MainViewState) {
    val context = LocalContext.current

    if (viewState.loginState == LoginState.Error) {
        LaunchedEffect(true) {
            Navigator.launchLoginActivity(context)
        }
    }

    FlatPage {
        val navController = rememberAnimatedNavController()
        var mainTab by remember { mutableStateOf(MainTab.Home) }

        if (viewState.loginState == LoginState.Login) {
            if (isTabletMode()) {
                MainTablet(navController, mainTab) { mainTab = it }
            } else {
                Main(navController, mainTab) { mainTab = it }
            }
        }
    }
}

@Composable
internal fun Main(navController: NavHostController, mainTab: MainTab, onTabSelected: (MainTab) -> Unit) {
    Column(Modifier) {
        AppNavigation(navController,
            Modifier
                .weight(1f)
                .fillMaxWidth())

        if (needShowBottomBar(navController)) {
            MainBottomBar(mainTab) { selectedTab ->
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
}

@Composable
internal fun MainTablet(navController: NavHostController, mainTab: MainTab, onTabSelected: (MainTab) -> Unit) {
    Row {
        Box(Modifier.width(56.dp)) {
            MainPadRail(selectedTab = mainTab) { selectedTab ->
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
        Box(Modifier.weight(1f), Alignment.Center) {
            when (mainTab) {
                MainTab.Home -> HomeScreen(
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
                    },
                    onOpenUserProfile = {
                        navController.navigate(LeafScreen.UserProfile.createRoute(Screen.HomeExt)) {
                            popUpTo(LeafScreen.HomeExtInit.createRoute(Screen.HomeExt))
                        }
                    },
                    onOpenSetting = {
                        navController.navigate(LeafScreen.Settings.createRoute(Screen.HomeExt)) {
                            popUpTo(LeafScreen.HomeExtInit.createRoute(Screen.HomeExt))
                        }
                    },
                )
                MainTab.CloudStorage -> CloudScreen(
                    onOpenUploading = {
                        navController.navigate(LeafScreen.CloudUploading.createRoute(Screen.CloudExt)) {
                            launchSingleTop = true
                        }
                    },
                    onOpenItemPick = {
                        navController.navigate(LeafScreen.CloudUploadPick.createRoute(Screen.CloudExt)) {
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
        Divider(MaxHeight.width(1.dp))
        AppNavigation(
            navController,
            modifier = Modifier
                .weight(2f)
                .fillMaxHeight(),
            startDestination = Screen.HomeExt.route,
        )
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
private fun MainBottomBar(selectedTab: MainTab, modifier: Modifier = Modifier, onTabSelected: (MainTab) -> Unit) {
    val homeResId = when (selectedTab) {
        MainTab.Home -> R.drawable.ic_home_main_selected
        MainTab.CloudStorage -> R.drawable.ic_home_main_normal
    }
    val csResId = when (selectedTab) {
        MainTab.CloudStorage -> R.drawable.ic_home_cloudstorage_selected
        MainTab.Home -> R.drawable.ic_home_cloudstorage_normal
    }

    Column(modifier) {
        FlatDivider()
        BottomAppBar(elevation = 0.dp, backgroundColor = MaterialTheme.colors.background) {
            Box(Modifier
                .weight(1f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = false, radius = 56.dp),
                    onClick = { onTabSelected(MainTab.Home) }
                ), Alignment.Center) {
                Image(painterResource(homeResId), null)
            }
            Box(Modifier
                .weight(1f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = false, radius = 56.dp),
                    onClick = { onTabSelected(MainTab.CloudStorage) }
                ), Alignment.Center) {
                Image(painterResource(csResId), null)
            }
        }
    }
}

@Composable
@Preview
@Preview(device = Devices.PIXEL_C)
private fun MainPagePreview() {
    val mainViewState = MainViewState(true, loginState = LoginState.Login)
    MainScreen(mainViewState)
}