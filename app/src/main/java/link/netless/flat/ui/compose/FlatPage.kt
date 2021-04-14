package link.netless.flat.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberAndroidSystemUiController
import link.netless.flat.ui.activity.ui.theme.FlatAndroidTheme
import link.netless.flat.ui.activity.ui.theme.FlatColorWhite

@Composable
fun FlatPage(statusBarColor: Color = FlatColorWhite, content: @Composable() () -> Unit) {
    val controller = rememberAndroidSystemUiController()
    controller.setStatusBarColor(statusBarColor)

    FlatAndroidTheme() {
        Surface(color = MaterialTheme.colors.background) {
            content()
        }
    }
}

@Composable
fun FlatColumnPage(
    modifier: Modifier = Modifier,
    statusBarColor: Color = FlatColorWhite,
    content: @Composable() ColumnScope.() -> Unit
) {
    val controller = rememberAndroidSystemUiController()
    controller.setStatusBarColor(statusBarColor)

    FlatAndroidTheme() {
        Surface(color = MaterialTheme.colors.background) {
            Column(modifier.fillMaxSize()) {
                content()
            }
        }
    }
}