package io.agora.flat.ui.theme

import android.annotation.SuppressLint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import io.agora.flat.util.isTabletMode


@SuppressLint("ConflictingOnColor")
private val DarkColorPalette = darkColors(
    primary = FlatColorBlue,
    primaryVariant = FlatColorBlue,

    secondary = FlatColorBlue,
    secondaryVariant = FlatColorBlue,

    // behind scrollable content
    background = Gray_10,
    // cards, sheets, and menus
    surface = FlatColorSurfaceDark,
    error = FlatColorRed,

    onPrimary = FlatColorTextPrimaryDark,
    onSecondary = FlatColorTextSecondaryDark,
    onBackground = FlatColorTextPrimaryDark,
    onSurface = FlatColorTextPrimaryDark,
    onError = FlatColorBlack
)

/**
 * See <a href="https://material.io/design/material-theming/implementing-your-theme.html">Material Theming</a>
 */
@SuppressLint("ConflictingOnColor")
private val LightColorPalette = lightColors(
    primary = FlatColorBlue,
    primaryVariant = FlatColorBlue,
    secondary = FlatColorBlue,
    secondaryVariant = FlatColorBlue,

    // behind scrollable content
    background = FlatColorWhite,
    // cards, sheets, and menus
    surface = FlatColorSurface,
    error = FlatColorRed,

    onPrimary = FlatColorTextPrimary,
    onSecondary = FlatColorTextSecondary,
    onBackground = FlatColorTextPrimary,
    onSurface = FlatColorTextPrimary,
    onError = FlatColorWhite
)

@Composable
fun FlatAndroidTheme(
    darkTheme: Boolean = isDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

@Composable
fun isTabletMode(): Boolean {
    return LocalContext.current.isTabletMode()
}

@Composable
fun isDarkTheme(): Boolean = isSystemInDarkTheme()

//@Composable
//fun isDarkTheme(): Boolean = when (DarkModeManager.current()) {
//    DarkModeManager.Mode.Auto -> isSystemInDarkTheme()
//    DarkModeManager.Mode.Light -> false
//    DarkModeManager.Mode.Dark -> true
//}
