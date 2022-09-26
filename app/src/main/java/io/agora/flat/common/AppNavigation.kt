package io.agora.flat.common

import androidx.compose.animation.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.*
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import io.agora.flat.ui.activity.cloud.list.CloudScreen
import io.agora.flat.ui.activity.cloud.uploading.Uploading
import io.agora.flat.ui.activity.history.HistoryScreen
import io.agora.flat.ui.activity.home.ExtInitPage
import io.agora.flat.ui.activity.home.HomeScreen
import io.agora.flat.ui.activity.room.CreateRoomScreen
import io.agora.flat.ui.activity.room.JoinRoomScreen
import io.agora.flat.ui.activity.room.RoomDetailScreen
import io.agora.flat.ui.activity.setting.SettingsScreen
import io.agora.flat.ui.activity.setting.UserProfile

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Cloud : Screen("cloud")
    object HomeExt : Screen("home_ext")
    object CloudExt : Screen("cloud_ext")
}

sealed class LeafScreen(private val route: String) {
    fun createRoute(root: Screen) = "${root.route}/$route"

    object Home : LeafScreen("home?roomUUID={room_uuid}") {
        // fun createRoute(root: Screen, roomUUID: String? = null): String {
        //     return "${root.route}/home".let {
        //         if (roomUUID != null) "$it?roomUUID=$roomUUID" else it
        //     }
        // }
    }

    object CloudStorage : LeafScreen("cloud")
    object HomeExtInit : LeafScreen("home_ext_init")
    object CloudExtInit : LeafScreen("cloud_ext_init")
    object CloudUploading : LeafScreen("uploading")
    object RoomJoin : LeafScreen("room_join")
    object RoomCreate : LeafScreen("room_create")

    object RoomDetail : LeafScreen("room_detail/{room_uuid}?periodicUUID={periodic_uuid}") {
        fun createRoute(root: Screen, roomUUID: String, periodicUUID: String? = null): String {
            return "${root.route}/room_detail/$roomUUID".let {
                if (periodicUUID != null) "$it?periodicUUID=$periodicUUID" else it
            }
        }
    }

    object UserProfile : LeafScreen("user_profile")
    object Settings : LeafScreen("settings")
    object History : LeafScreen("history")
}


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Home.route,
) {
    AnimatedNavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { defaultEnterTransition(this.initialState, this.targetState) },
        exitTransition = { defaultExitTransition(this.initialState, this.targetState) },
        popEnterTransition = { defaultPopEnterTransition(this.initialState, this.targetState) },
        popExitTransition = { defaultPopExitTransition(this.initialState, this.targetState) },
        modifier = modifier,
    ) {
        addHomeGraph(navController)
        addCloudGraph(navController)
        addHomeExtGraph(navController)
        addCloudExtGraph(navController)
    }
}

@OptIn(ExperimentalAnimationApi::class)
private fun NavGraphBuilder.addHomeExtGraph(navController: NavHostController) {
    val screenRoot = Screen.HomeExt
    navigation(
        route = Screen.HomeExt.route,
        startDestination = LeafScreen.HomeExtInit.createRoute(screenRoot),
    ) {
        composable(LeafScreen.HomeExtInit.createRoute(screenRoot)) {
            ExtInitPage()
        }
        composable(LeafScreen.RoomCreate.createRoute(screenRoot)) {
            CreateRoomScreen(navController)
        }
        composable(LeafScreen.RoomJoin.createRoute(screenRoot)) {
            JoinRoomScreen(navController)
        }
        composable(
            LeafScreen.RoomDetail.createRoute(screenRoot),
            arguments = listOf(navArgument("room_uuid") {
                type = NavType.StringType
            })
        ) {
            RoomDetailScreen(navController)
        }
        composable(LeafScreen.Settings.createRoute(screenRoot)) {
            SettingsScreen(navController)
        }
        composable(LeafScreen.UserProfile.createRoute(screenRoot)) {
            UserProfile(navController)
        }
        composable(LeafScreen.History.createRoute(screenRoot)) {
            HistoryScreen(
                onBackPressed = {
                    navController.popBackStack()
                },
                onOpenRoomDetail = { roomUUID, periodicUUID ->
                    navController.navigate(LeafScreen.RoomDetail.createRoute(screenRoot, roomUUID, periodicUUID))
                }
            )
        }
    }
}


@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.addCloudExtGraph(navController: NavHostController) {
    navigation(
        route = Screen.CloudExt.route,
        startDestination = LeafScreen.CloudExtInit.createRoute(Screen.CloudExt),
    ) {
        composable(LeafScreen.CloudExtInit.createRoute(Screen.CloudExt)) {
            ExtInitPage()
        }
        composable(LeafScreen.CloudUploading.createRoute(Screen.CloudExt)) {
            Uploading(
                onCloseUploading = navController::popBackStack
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.addCloudGraph(navController: NavHostController) {
    val screenRoot = Screen.Cloud
    navigation(route = screenRoot.route, startDestination = LeafScreen.CloudStorage.createRoute(screenRoot)) {
        composable(LeafScreen.CloudStorage.createRoute(screenRoot)) {
            CloudScreen(
                onOpenUploading = {
                    navController.navigate(LeafScreen.CloudUploading.createRoute(screenRoot)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(LeafScreen.CloudUploading.createRoute(screenRoot)) {
            Uploading(onCloseUploading = { navController.popBackStack() })
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.addHomeGraph(navController: NavController) {
    val screenRoot = Screen.Home
    navigation(route = Screen.Home.route, startDestination = LeafScreen.Home.createRoute(screenRoot)) {
        composable(LeafScreen.Home.createRoute(screenRoot)) {
            HomeScreen(
                onOpenRoomCreate = {
                    navController.navigate(LeafScreen.RoomCreate.createRoute(screenRoot))
                },
                onOpenRoomJoin = {
                    navController.navigate(LeafScreen.RoomJoin.createRoute(screenRoot))
                },
                onOpenRoomDetail = { roomUUID, periodicUUID ->
                    navController.navigate(LeafScreen.RoomDetail.createRoute(screenRoot, roomUUID, periodicUUID))
                },
                onOpenUserProfile = {
                    navController.navigate(LeafScreen.UserProfile.createRoute(screenRoot))
                },
                onOpenSetting = {
                    navController.navigate(LeafScreen.Settings.createRoute(screenRoot))
                },
                onOpenHistory = {
                    navController.navigate(LeafScreen.History.createRoute(screenRoot))
                }
            )
        }
        composable(LeafScreen.RoomCreate.createRoute(screenRoot)) {
            CreateRoomScreen(navController)
        }
        composable(LeafScreen.RoomJoin.createRoute(screenRoot)) {
            JoinRoomScreen(navController)
        }
        composable(
            LeafScreen.RoomDetail.createRoute(screenRoot),
            arguments = listOf(navArgument("room_uuid") { type = NavType.StringType })
        ) {
            RoomDetailScreen(navController)
        }
        composable(LeafScreen.Settings.createRoute(screenRoot)) {
            SettingsScreen(navController)
        }
        composable(LeafScreen.UserProfile.createRoute(screenRoot)) {
            UserProfile(navController)
        }
        composable(LeafScreen.History.createRoute(screenRoot)) {
            HistoryScreen(
                onBackPressed = {
                    navController.popBackStack()
                },
                onOpenRoomDetail = { roomUUID, periodicUUID ->
                    navController.navigate(LeafScreen.RoomDetail.createRoute(screenRoot, roomUUID, periodicUUID))
                }
            )
        }
    }
}

@Composable
internal fun needShowBottomBar(navController: NavHostController): Boolean {
    val showRoutes = setOf(
        LeafScreen.Home.createRoute(Screen.Home),
        LeafScreen.CloudStorage.createRoute(Screen.Cloud)
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    return showRoutes.contains(navBackStackEntry?.destination?.route)
}

@ExperimentalAnimationApi
private fun AnimatedContentScope<*>.defaultEnterTransition(
    initial: NavBackStackEntry,
    target: NavBackStackEntry,
): EnterTransition {
    val initialNavGraph = initial.destination.hostNavGraph
    val targetNavGraph = target.destination.hostNavGraph
    // If we're crossing nav graphs (bottom navigation graphs), we crossfade
    if (initialNavGraph.id != targetNavGraph.id) {
        return fadeIn()
    }
    // Otherwise we're in the same nav graph, we can imply a direction
    return fadeIn() + slideIntoContainer(AnimatedContentScope.SlideDirection.Start)
}

@ExperimentalAnimationApi
private fun AnimatedContentScope<*>.defaultExitTransition(
    initial: NavBackStackEntry,
    target: NavBackStackEntry,
): ExitTransition {
    val initialNavGraph = initial.destination.hostNavGraph
    val targetNavGraph = target.destination.hostNavGraph
    // If we're crossing nav graphs (bottom navigation graphs), we crossfade
    if (initialNavGraph.id != targetNavGraph.id) {
        return fadeOut()
    }
    // Otherwise we're in the same nav graph, we can imply a direction
    return fadeOut()
}

private val NavDestination.hostNavGraph: NavGraph
    get() = hierarchy.first { it is NavGraph } as NavGraph

@ExperimentalAnimationApi
private fun AnimatedContentScope<*>.defaultPopEnterTransition(
    initial: NavBackStackEntry,
    target: NavBackStackEntry,
): EnterTransition {
    val initialNavGraph = initial.destination.hostNavGraph
    val targetNavGraph = target.destination.hostNavGraph
    // If we're crossing nav graphs (bottom navigation graphs), we crossfade
    if (initialNavGraph.id != targetNavGraph.id) {
        return fadeIn()
    }
    return fadeIn()
}

@ExperimentalAnimationApi
private fun AnimatedContentScope<*>.defaultPopExitTransition(
    initial: NavBackStackEntry,
    target: NavBackStackEntry,
): ExitTransition {
    val initialNavGraph = initial.destination.hostNavGraph
    val targetNavGraph = target.destination.hostNavGraph
    // If we're crossing nav graphs (bottom navigation graphs), we crossfade
    if (initialNavGraph.id != targetNavGraph.id) {
        return fadeOut()
    }
    return fadeOut() + slideOutOfContainer(AnimatedContentScope.SlideDirection.End)
}
