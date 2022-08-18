package io.agora.flat.ui.activity.home

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.R
import io.agora.flat.common.*
import io.agora.flat.common.login.LoginManager
import io.agora.flat.common.version.VersionCheckResult
import io.agora.flat.di.interfaces.Crashlytics
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.compose.*
import io.agora.flat.ui.theme.*
import io.agora.flat.ui.util.ShowUiMessageEffect
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BaseComposeActivity() {
    @Inject
    lateinit var loginManager: LoginManager

    @Inject
    lateinit var crashlytics: Crashlytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel: MainViewModel by viewModels()
            val viewState by viewModel.state.collectAsState()
            val roomPlayInfo by viewModel.roomPlayInfo.collectAsState()

            if (viewState.protocolAgreed) {
                MainScreen(viewState)
                LifecycleHandler(
                    onCreate = {
                        loginManager.registerApp()
                        loginManager.registerReceiver(this)
                        crashlytics.init(applicationContext)
                    },
                    onResume = {
                        if (!viewModel.isLoggedIn() || viewModel.needBindPhone()) {
                            Navigator.launchLoginActivity(this@MainActivity)
                        }
                        viewModel.checkVersion()
                    },
                    onDestroy = {
                        loginManager.unregisterReceiver(this)
                    },
                )
            } else {
                GlobalAgreementDialog(
                    onAgree = { viewModel.agreeProtocol() },
                    onRefuse = { finish() },
                )
            }

            if (viewState.versionCheckResult.showUpdate) {
                UpdateDialog(
                    viewState.versionCheckResult,
                    viewState.updating,
                    viewModel::updateApp,
                    viewModel::cancelUpdate,
                )
            }

            ShowUiMessageEffect(uiMessage = viewState.message, onMessageShown = viewModel::clearMessage)

            LaunchedEffect(roomPlayInfo) {
                roomPlayInfo?.let {
                    Navigator.launchRoomPlayActivity(this@MainActivity, it)
                }
            }

            if (viewState.loginState == LoginState.Error) {
                LaunchedEffect(true) {
                    Navigator.launchLoginActivity(this@MainActivity)
                }
            }
        }
    }
}

@Composable
internal fun UpdateDialog(
    versionCheckResult: VersionCheckResult,
    updating: Boolean,
    onUpdate: () -> Unit,
    onCancel: () -> Unit,
) {
    FlatAndroidTheme {
        val dismissBlocked = {}
        Dialog(onDismissRequest = if (versionCheckResult.forceUpdate) dismissBlocked else onCancel) {
            Surface(shape = Shapes.large) {
                Column(Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                    FlatTextTitle(versionCheckResult.title)
                    FlatNormalVerticalSpacer()
                    FlatTextBodyOneSecondary(versionCheckResult.description)
                    FlatNormalVerticalSpacer()

                    if (updating) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            color = MaterialTheme.colors.primary,
                        )
                    } else {
                        Column {
                            FlatPrimaryTextButton(stringResource(R.string.update)) {
                                onUpdate()
                            }
                            if (!versionCheckResult.forceUpdate) {
                                FlatSmallVerticalSpacer()
                                FlatSecondaryTextButton(stringResource(R.string.cancel)) {
                                    onCancel()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainScreen(viewState: MainUiState) {
    FlatPage {
        val navController = rememberAnimatedNavController()
        val selectTab by navController.currentTabAsState()

        if (viewState.loginState == LoginState.Login) {
            if (isTabletMode()) {
                MainTablet(navController, selectTab)
            } else {
                Main(navController, selectTab)
            }
        }
    }
}


@Stable
@Composable
private fun NavController.currentTabAsState(): State<MainTab> {
    val currentTab = remember { mutableStateOf(MainTab.Home) }

    DisposableEffect(this) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            when {
                destination.hierarchy.any { it.route == Screen.Home.route || it.route == Screen.HomeExt.route } -> {
                    currentTab.value = MainTab.Home
                }
                destination.hierarchy.any { it.route == Screen.Cloud.route || it.route == Screen.CloudExt.route } -> {
                    currentTab.value = MainTab.Cloud
                }
            }
        }
        addOnDestinationChangedListener(listener)

        onDispose {
            removeOnDestinationChangedListener(listener)
        }
    }

    return currentTab
}

private fun NavController.currentIsRoute(route: String): Boolean {
    return NavDestination.createRoute(route)
        .toUri() == (currentBackStackEntry?.arguments?.get(NavController.KEY_DEEP_LINK_INTENT) as? Intent)?.data
}

@Composable
internal fun Main(navController: NavHostController, mainTab: MainTab) {
    Column {
        AppNavHost(
            navController,
            Modifier
                .weight(1f)
                .fillMaxWidth()
        )

        if (needShowBottomBar(navController)) {
            MainBottomBar(mainTab) { selectedTab ->
                val route = when (selectedTab) {
                    MainTab.Home -> Screen.Home.route
                    MainTab.Cloud -> Screen.Cloud.route
                }

                navController.navigate(route) {
                    launchSingleTop = true
                    restoreState = true

                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                }
            }
        }
    }
}

@Composable
internal fun MainTablet(navController: NavHostController, mainTab: MainTab) {
    Row {
        Box(Modifier.width(56.dp)) {
            MainPadRail(selectedTab = mainTab) { selectedTab ->
                val route = when (selectedTab) {
                    MainTab.Home -> Screen.HomeExt.route
                    MainTab.Cloud -> Screen.CloudExt.route
                }

                navController.navigate(route) {
                    launchSingleTop = true
                    restoreState = true

                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                }
            }
        }
        Box(Modifier.weight(1f), Alignment.Center) {
            when (mainTab) {
                MainTab.Home -> HomeScreen(
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
                        val route = LeafScreen.RoomDetail.createRoute(
                            Screen.HomeExt,
                            rUUID,
                            pUUID,
                        )
                        if (navController.currentIsRoute(route)) {
                            return@HomeScreen
                        }
                        navController.navigate(route) {
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
                MainTab.Cloud -> CloudScreen(
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
        AppNavHost(
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
        MainTab.Cloud -> R.drawable.ic_home_main_normal
    }
    val csResId = when (selectedTab) {
        MainTab.Cloud -> R.drawable.ic_home_cloudstorage_selected
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
            IconButton(onClick = { onTabSelected(MainTab.Cloud) }) {
                Image(painterResource(csResId), null)
            }
        }
    }
}

@Composable
private fun MainBottomBar(selectedTab: MainTab, modifier: Modifier = Modifier, onTabSelected: (MainTab) -> Unit) {
    val homeResId = when (selectedTab) {
        MainTab.Home -> R.drawable.ic_home_main_selected
        MainTab.Cloud -> R.drawable.ic_home_main_normal
    }
    val csResId = when (selectedTab) {
        MainTab.Cloud -> R.drawable.ic_home_cloudstorage_selected
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
                    onClick = { onTabSelected(MainTab.Cloud) }
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
    val mainViewState = MainUiState(true, loginState = LoginState.Login)
    MainScreen(mainViewState)
}