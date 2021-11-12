package io.agora.flat.ui.activity.home.mainext

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navArgument
import io.agora.flat.R
import io.agora.flat.ui.activity.room.CreateRoomPage
import io.agora.flat.ui.activity.room.JoinRoomPage
import io.agora.flat.ui.activity.room.RoomDetailPage
import io.agora.flat.ui.activity.setting.SettingPage

@Composable
fun MainExtPage(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "init") {
        composable("init") {
            MainExtInitPage()
        }

        composable("room_create") {
            CreateRoomPage(navController)
        }

        composable("room_join") {
            JoinRoomPage(navController)
        }
        composable("room_detail?room_uuid={room_uuid}",
            arguments = listOf(navArgument("room_uuid") {
                type = NavType.StringType
            })) {
            RoomDetailPage(navController)
        }
        composable("setting") {
            SettingPage()
        }
    }
}

@Composable
internal fun MainExtInitPage() {
    Box(Modifier.fillMaxSize()) {
        Image(
            painterResource(R.drawable.img_pad_home_ext_empty),
            contentDescription = null,
            Modifier.align(Alignment.Center)
        )
    }
}