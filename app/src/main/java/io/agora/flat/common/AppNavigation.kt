package io.agora.flat.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navArgument
import io.agora.flat.ui.activity.home.*
import io.agora.flat.ui.activity.room.CreateRoomPage
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


@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Home.route,
) {
    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
        addHomeGraph(navController)
        addCloudGraph(navController)
        addHomeExtGraph(navController)
        addCloudExtGraph(navController)
    }
}

private fun NavGraphBuilder.addHomeExtGraph(navController: NavHostController) {
    val screenRoot = Screen.HomeExt
    navigation(route = Screen.HomeExt.route,
        startDestination = LeafScreen.HomeExtInit.createRoute(Screen.HomeExt)) {
        composable(LeafScreen.HomeExtInit.createRoute(Screen.HomeExt)) {
            ExtInitPage()
        }
        composable(LeafScreen.RoomCreate.createRoute(screenRoot)) {
            CreateRoomPage(navController)
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

fun NavGraphBuilder.addHomeGraph(navController: NavController) {
    val screenRoot = Screen.Home
    navigation(route = Screen.Home.route, startDestination = LeafScreen.Home.createRoute(screenRoot)) {
        composable(LeafScreen.Home.createRoute(screenRoot)) {
            Home(
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
            CreateRoomPage(navController)
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
