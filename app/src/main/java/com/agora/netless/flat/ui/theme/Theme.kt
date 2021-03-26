package com.agora.netless.flat.ui.activity.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable

private val DarkColorPalette = darkColors(

)


/**
 * See <a href="https://material.io/design/material-theming/implementing-your-theme.html">Material Theming</a>
 */
private val LightColorPalette = lightColors(
    primary = FlatWhite,
    primaryVariant = FlatWhite,
    secondary = FlatBlue,
    secondaryVariant = FlatBlue,

    // behind scrollable content
    background = FlatWhite,
    // cards, sheets, and menus
    surface = FlatWhite,
    error = FlatRed,

    onPrimary = FlatTextPrimary,
    onSecondary = FlatTextSecondary,
    onBackground = FlatBlack,
    onSurface = FlatBlack,
    onError = FlatRed
)

@Composable
fun FlatAndroidTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable() () -> Unit) {
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