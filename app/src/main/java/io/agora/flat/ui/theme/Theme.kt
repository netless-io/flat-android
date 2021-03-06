package io.agora.flat.ui.theme

import android.annotation.SuppressLint
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
@SuppressLint("ConflictingOnColor")
private val LightColorPalette = lightColors(
    primary = FlatColorBlue,
    primaryVariant = FlatColorBlue,
    secondary = FlatColorBlue,
    secondaryVariant = FlatColorBlue,

    // behind scrollable content
    background = FlatColorWhite,
    // cards, sheets, and menus
    surface = FlatColorWhite,
    error = FlatColorRed,

    onPrimary = FlatColorTextPrimary,
    onSecondary = FlatColorTextSecondary,
    onBackground = FlatColorBlack,
    onSurface = FlatColorBlack,
    onError = FlatColorRed
)

@Composable
fun FlatAndroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable() () -> Unit
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