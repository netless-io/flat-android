package com.agora.netless.flat.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.agora.netless.flat.ui.activity.ui.theme.FlatAndroidTheme
import com.agora.netless.flat.ui.activity.ui.theme.FlatWhite
import com.google.accompanist.systemuicontroller.rememberAndroidSystemUiController

@Composable
fun FlatPage(statusBarColor: Color = FlatWhite, content: @Composable() () -> Unit) {
    val controller = rememberAndroidSystemUiController()
    controller.setStatusBarColor(statusBarColor)

    FlatAndroidTheme() {
        Surface(color = MaterialTheme.colors.background) {
            content()
        }
    }
}

@Composable
fun FlatColumnPage(statusBarColor: Color = FlatWhite, content: @Composable() ColumnScope.() -> Unit) {
    val controller = rememberAndroidSystemUiController()
    controller.setStatusBarColor(statusBarColor)

    FlatAndroidTheme() {
        Surface(color = MaterialTheme.colors.background) {
            Column(Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}