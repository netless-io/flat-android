package io.agora.flat.common

import androidx.compose.animation.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.*
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navArgument
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import io.agora.flat.ui.activity.home.*
import io.agora.flat.ui.activity.room.CreateRoomScreen
import io.agora.flat.ui.activity.room.JoinRoomPage
import io.agora.flat.ui.activity.room.RoomDetailPage
import io.agora.flat.ui.activity.setting.Settings
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

    object CloudStorage : LeafScreen("cloudstorage")
    object HomeExtInit : LeafScreen("home_ext_init")
    object CloudExtInit : LeafScreen("cloud_ext_init")
    object CloudUploadPick : LeafScreen("upload_pick")
    object CloudUploading : LeafScreen("uploading")
    object RoomJoin : LeafScreen("roomjoin")
    object RoomCreate : LeafScreen("roomcreate")

    object RoomDetail : LeafScreen("roomdetail/{room_uuid}?periodicUUID={periodic_uuid}") {
        fun createRoute(root: Screen, roomUUID: String, periodicUUID: String? = null): String {
            return "${root.route}/roomdetail/$roomUUID".let {
                if (periodicUUID != null) "$it?periodicUUID=$periodicUUID" else it
            }
        }
    }

    object UserProfile : LeafScreen("user_profile")
    object Settings : LeafScreen("settings")
}


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Home.route,
) {
    AnimatedNavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { initial, target -> defaultEnterTransition(initial, target) },
        exitTransition = { initial, target -> defaultExitTransition(initial, target) },
        popEnterTransition = { _, _ -> defaultPopEnterTransition() },
        popExitTransition = { _, _ -> defaultPopExitTransition() },
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
    navigation(route = Screen.HomeExt.route,
        startDestination = LeafScreen.HomeExtInit.createRoute(Screen.HomeExt)) {
        composable(LeafScreen.HomeExtInit.createRoute(Screen.HomeExt)) {
            ExtInitPage()
        }
        composable(LeafScreen.RoomCreate.createRoute(screenRoot)) {
            CreateRoomScreen(navController)
        }
        composable(LeafScreen.RoomJoin.createRoute(screenRoot)) {
            JoinRoomPage(navController)
        }
        composable(LeafScreen.RoomDetail.createRoute(screenRoot),
            arguments = listOf(navArgument("room_uuid") {
                type = NavType.StringType
            })) {
            RoomDetailPage(navController)
        }
        composable(LeafScreen.Settings.createRoute(screenRoot)) {
            Settings(navController)
        }
        composable(LeafScreen.UserProfile.createRoute(screenRoot)) {
            UserProfile(navController)
        }
    }
}


@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.addCloudExtGraph(navController: NavHostController) {
    navigation(route = Screen.CloudExt.route,
        startDestination = LeafScreen.CloudExtInit.createRoute(Screen.CloudExt)) {
        composable(LeafScreen.CloudExtInit.createRoute(Screen.CloudExt)) {
            ExtInitPage()
        }
        composable(LeafScreen.CloudUploadPick.createRoute(Screen.CloudExt)) {
            CloudUploadPick(
                onPickClose = navController::popBackStack,
            )
        }
        composable(LeafScreen.CloudUploading.createRoute(Screen.CloudExt)) {
            UploadList(
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
            CloudStorage(
                onOpenUploading = {
                    navController.navigate(LeafScreen.CloudUploading.createRoute(screenRoot)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(LeafScreen.CloudUploading.createRoute(screenRoot)) {
            UploadList(onCloseUploading = { navController.popBackStack() })
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.addHomeGraph(navController: NavController) {
    val screenRoot = Screen.Home
    navigation(route = Screen.Home.route, startDestination = LeafScreen.Home.createRoute(screenRoot)) {
        composable(LeafScreen.Home.createRoute(screenRoot)) {
            HomeScreen(
                navController,
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
            )
        }
        composable(LeafScreen.RoomCreate.createRoute(screenRoot)) {
            CreateRoomScreen(navController)
        }
        composable(LeafScreen.RoomJoin.createRoute(screenRoot)) {
            JoinRoomPage(navController)
        }
        composable(LeafScreen.RoomDetail.createRoute(screenRoot),
            arguments = listOf(navArgument("room_uuid") {
                type = NavType.StringType
            })) {
            RoomDetailPage(navController)
        }
        composable(LeafScreen.Settings.createRoute(screenRoot)) {
            Settings(navController)
        }
        composable(LeafScreen.UserProfile.createRoute(screenRoot)) {
            UserProfile(navController)
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
    return fadeOut() + slideOutOfContainer(AnimatedContentScope.SlideDirection.Start)
}

private val NavDestination.hostNavGraph: NavGraph
    get() = hierarchy.first { it is NavGraph } as NavGraph

@ExperimentalAnimationApi
private fun AnimatedContentScope<*>.defaultPopEnterTransition(): EnterTransition {
    return fadeIn() + slideIntoContainer(AnimatedContentScope.SlideDirection.End)
}

@ExperimentalAnimationApi
private fun AnimatedContentScope<*>.defaultPopExitTransition(): ExitTransition {
    return fadeOut() + slideOutOfContainer(AnimatedContentScope.SlideDirection.End)
}
