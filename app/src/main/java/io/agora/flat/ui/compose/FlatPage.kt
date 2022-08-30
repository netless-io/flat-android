package io.agora.flat.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import io.agora.flat.ui.theme.FlatTheme

@Composable
fun FlatPage(
    statusBarColor: Color? = null,
    content: @Composable () -> Unit,
) {
    FlatTheme {
        val systemUiController = rememberSystemUiController()
        val useDarkIcons = MaterialTheme.colors.isLight
        val background = MaterialTheme.colors.background

        SideEffect {
            systemUiController.setSystemBarsColor(
                color = statusBarColor ?: background,
                darkIcons = useDarkIcons
            )
        }

        ProvideWindowInsets(consumeWindowInsets = false) {
            Surface(color = MaterialTheme.colors.background) {
                content()
            }
        }
    }
}

@Composable
fun FlatColumnPage(
    statusBarColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    FlatTheme {
        val systemUiController = rememberSystemUiController()
        val useDarkIcons = MaterialTheme.colors.isLight
        val background = MaterialTheme.colors.background

        SideEffect {
            systemUiController.setSystemBarsColor(
                color = statusBarColor ?: background,
                darkIcons = useDarkIcons
            )
        }

        Surface(color = MaterialTheme.colors.background) {
            Column(Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}