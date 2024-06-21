package io.agora.flat.ui.activity.home

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.AppInitializers
import io.agora.flat.PostLoginInitializers
import io.agora.flat.R
import io.agora.flat.common.*
import io.agora.flat.common.login.LoginManager
import io.agora.flat.common.rtc.AgoraRtc
import io.agora.flat.data.AppKVCenter
import io.agora.flat.di.interfaces.RtcApi
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.activity.cloud.list.CloudScreen
import io.agora.flat.ui.compose.*
import io.agora.flat.ui.theme.isTabletMode
import io.agora.flat.ui.util.ShowUiMessageEffect
import io.agora.flat.util.launchMarket
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BaseComposeActivity() {
    @Inject
    lateinit var loginManager: LoginManager

    @Inject
    lateinit var initializers: AppInitializers

    @Inject
    lateinit var postLoginInitializers: PostLoginInitializers

    @Inject
    lateinit var rtcApi: RtcApi

    @Inject
    lateinit var appKVCenter: AppKVCenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel: MainViewModel by viewModels()
            val viewState by viewModel.state.collectAsState()
            val roomPlayInfo by viewModel.roomPlayInfo.collectAsState()
            val replayInfo by viewModel.replayInfo.collectAsState()

            if (viewState.protocolAgreed) {
                CompositionLocalProvider(
                    LocalAgoraRtc provides rtcApi as? AgoraRtc,
                    LocalAppKVCenter provides appKVCenter
                ) {
                    MainScreen(viewState)
                }
                LifecycleHandler(
                    onCreate = {
                        loginManager.registerApp()
                        loginManager.registerReceiver(this)
                        // init after protocol agreed
                        initializers.init()
                    },
                    onResume = {
                        if (!viewModel.isLoggedIn()) {
                            Navigator.launchLoginActivity(this@MainActivity)
                            return@LifecycleHandler
                        }

                        if (viewModel.needBindPhone()) {
                            Navigator.launchPhoneBindActivity(this@MainActivity)
                            return@LifecycleHandler
                        }

                        viewModel.checkVersion()

                        intent?.data?.let { data ->
                            viewModel.handleDeepLink(data)
                            intent.data = null
                        }
                        postLoginInitializers.init()
                    },
                    onDestroy = {
                        loginManager.unregisterReceiver(this)
                    },
                )

                LaunchedEffect(roomPlayInfo) {
                    roomPlayInfo?.let {
                        viewModel.cleanRoomPlayInfo()
                        Navigator.launchRoomPlayActivity(this@MainActivity, it)
                    }
                }

                LaunchedEffect(replayInfo) {
                    replayInfo?.let {
                        viewModel.clearReplayInfo()
                        Navigator.launchPlaybackActivity(this@MainActivity, it)
                    }
                }

                if (viewState.loginState == LoginState.Error) {
                    LaunchedEffect(true) {
                        Navigator.launchLoginActivity(this@MainActivity)
                    }
                }

                if (viewState.versionCheckResult.showUpdate) {
                    UpdateDialog(
                        viewState.versionCheckResult,
                        viewModel::downloadApp,
                        viewModel::cancelUpdate,
                        onGotoMarket = {
                            this.launchMarket()
                            viewModel.cancelUpdate()
                        }
                    )
                }
            } else {
                FlatPage {
                    GlobalAgreementDialog(
                        onAgree = { viewModel.agreeProtocol() },
                        onRefuse = { finish() },
                    )
                }
            }

            ShowUiMessageEffect(uiMessage = viewState.message, onMessageShown = viewModel::clearMessage)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
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
                MainTablet(navController, selectTab, viewState.userAvatar)
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
    var bottomBarState by rememberSaveable { (mutableStateOf(true)) }

    bottomBarState = needShowBottomBar(navController = navController)
    val height: Int by animateIntAsState(if (bottomBarState) 56 else 0)

    Column {
        AppNavHost(
            navController,
            Modifier
                .weight(1f)
                .fillMaxWidth()
        )

        MainBottomBar(mainTab, Modifier.height(height.dp)) { selectedTab ->
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

@Composable
internal fun MainTablet(navController: NavHostController, mainTab: MainTab, avatar: String?) {
    val context = LocalContext.current

    val onOpenSetting = {
        navController.navigate(LeafScreen.Settings.createRoute(Screen.HomeExt)) {
            popUpTo(LeafScreen.HomeExtInit.createRoute(Screen.HomeExt))
        }
    }
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
            if (mainTab == MainTab.Home) {
                IconButton(
                    onClick = onOpenSetting,
                    Modifier
                        .padding(vertical = 16.dp)
                        .align(Alignment.TopCenter)
                ) {
                    FlatAvatar(avatar, size = 32.dp)
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
                    onOpenHistory = {
                        navController.navigate(LeafScreen.History.createRoute(Screen.HomeExt)) {
                            popUpTo(LeafScreen.HomeExtInit.createRoute(Screen.HomeExt))
                        }
                    }
                )

                MainTab.Cloud -> CloudScreen(
                    onOpenUploading = {
                        navController.navigate(LeafScreen.CloudUploading.createRoute(Screen.CloudExt)) {
                            launchSingleTop = true
                        }
                    },
                )
            }
        }
        FlatDivider(
            Modifier
                .fillMaxHeight()
                .width(1.dp)
        )
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
        MainTab.Home -> R.drawable.ic_main_home_selected
        MainTab.Cloud -> R.drawable.ic_main_home_normal
    }
    val csResId = when (selectedTab) {
        MainTab.Cloud -> R.drawable.ic_main_cloud_selected
        MainTab.Home -> R.drawable.ic_main_cloud_normal
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
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
private fun MainBottomBar(
    selectedTab: MainTab,
    modifier: Modifier = Modifier,
    onTabSelected: (MainTab) -> Unit,
) {
    val homeResId = when (selectedTab) {
        MainTab.Home -> R.drawable.ic_main_home_selected
        MainTab.Cloud -> R.drawable.ic_main_home_normal
    }
    val csResId = when (selectedTab) {
        MainTab.Cloud -> R.drawable.ic_main_cloud_selected
        MainTab.Home -> R.drawable.ic_main_cloud_normal
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