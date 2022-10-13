package io.agora.flat.ui.theme

import android.annotation.SuppressLint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import io.agora.flat.common.android.DarkModeManager
import io.agora.flat.util.isTabletMode


@SuppressLint("ConflictingOnColor")
private val DarkColorPalette = darkColors(
    primary = Blue_7,
    primaryVariant = Blue_7,

    secondary = Blue_7,
    secondaryVariant = Blue_7,

    // behind scrollable content
    background = Gray_10,
    // cards, sheets, and menus
    surface = Gray_9,
    error = Red_7,

    onPrimary = Gray_0,
    onSecondary = Gray_4,
    onBackground = Gray_3,
    onSurface = Gray_3,
    onError = FlatColorBlack
)

/**
 * See <a href="https://material.io/design/material-theming/implementing-your-theme.html">Material Theming</a>
 */
@SuppressLint("ConflictingOnColor")
private val LightColorPalette = lightColors(
    primary = Blue_6,
    primaryVariant = Blue_6,
    secondary = Blue_6,
    secondaryVariant = Blue_6,

    // behind scrollable content
    background = FlatColorWhite,
    // cards, sheets, and menus
    surface = FlatColorWhite,
    error = Red_6,

    onPrimary = Gray_12,
    onSecondary = Gray_6,
    onBackground = Gray_6,
    onSurface = Gray_6,
    onError = FlatColorWhite
)

private val LightExtendedColors = ExtendedColors(
    textPrimary = Gray_6,
    textSecondary = Gray_3,
    textTitle = Blue_12,
    divider = Gray_1,
)

private val DarkExtendedColors = ExtendedColors(
    textPrimary = Gray_3,
    textSecondary = Gray_6,
    textTitle = Blue_0,
    divider = Gray_8,
)

@Composable
fun FlatTheme(
    darkTheme: Boolean = isDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColorPalette else LightColorPalette
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors
    CompositionLocalProvider(
        LocalExtendedColors provides extendedColors,
    ) {
        MaterialTheme(
            colors = colors,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}

object FlatTheme {
    val colors: ExtendedColors
        @Composable
        get() = LocalExtendedColors.current
}

@Immutable
data class ExtendedColors(
    val textPrimary: Color,
    val textSecondary: Color,
    val textTitle: Color,
    val divider: Color,
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        textPrimary = Color.Unspecified,
        textSecondary = Color.Unspecified,
        textTitle = Color.Unspecified,
        divider = Color.Unspecified
    )
}

@Composable
fun isTabletMode(): Boolean {
    return LocalContext.current.isTabletMode()
}

@Composable
fun isDarkTheme(): Boolean = when (DarkModeManager.current()) {
    DarkModeManager.Mode.Auto -> isSystemInDarkTheme()
    DarkModeManager.Mode.Light -> false
    DarkModeManager.Mode.Dark -> true
}