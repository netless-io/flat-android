package io.agora.flat.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import io.agora.flat.ui.theme.FlatAndroidTheme

@Composable
fun FlatPage(
    statusBarColor: Color = MaterialTheme.colors.background,
    content: @Composable() () -> Unit,
) {
    val controller = rememberSystemUiController()
    controller.setStatusBarColor(statusBarColor)

    FlatAndroidTheme {
        Surface(color = MaterialTheme.colors.background) {
            Box(Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}

@Composable
fun FlatColumnPage(
    statusBarColor: Color = MaterialTheme.colors.background,
    content: @Composable() ColumnScope.() -> Unit,
) {
    val controller = rememberSystemUiController()
    controller.setStatusBarColor(statusBarColor)

    FlatAndroidTheme {
        Surface(color = MaterialTheme.colors.background) {
            Column(Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}