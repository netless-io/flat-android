package io.agora.flat.ui.activity.play

import androidx.annotation.DrawableRes
import io.agora.flat.R

data class ColorItem(@DrawableRes val drawableRes: Int, val color: IntArray) {
    companion object {
        var colors = listOf(
            ColorItem(R.drawable.ic_toolbox_color_red, arrayOf(0xE0, 0x20, 0x20).toIntArray()),
            ColorItem(R.drawable.ic_toolbox_color_orange, arrayOf(0xFA, 0x64, 0x00).toIntArray()),
            ColorItem(R.drawable.ic_toolbox_color_yellow, arrayOf(0xF7, 0xB5, 0x00).toIntArray()),
            ColorItem(R.drawable.ic_toolbox_color_green, arrayOf(0x6D, 0xD4, 0x00).toIntArray()),
            ColorItem(R.drawable.ic_toolbox_color_cyan, arrayOf(0x44, 0xD7, 0xB6).toIntArray()),
            ColorItem(R.drawable.ic_toolbox_color_blue, arrayOf(0x00, 0x91, 0xFF).toIntArray()),
            ColorItem(R.drawable.ic_toolbox_color_deep_purple, arrayOf(0x62, 0x36, 0xFF).toIntArray()),
            ColorItem(R.drawable.ic_toolbox_color_purple, arrayOf(0xB6, 0xC0, 0xC6).toIntArray()),
            ColorItem(R.drawable.ic_toolbox_color_blue_gray, arrayOf(0xBC, 0x20, 0xE0).toIntArray()),
            ColorItem(R.drawable.ic_toolbox_color_gray, arrayOf(0x6D, 0x72, 0x78).toIntArray()),
            ColorItem(R.drawable.ic_toolbox_color_black, arrayOf(0x00, 0x00, 0x00).toIntArray()),
            ColorItem(R.drawable.ic_toolbox_color_white, arrayOf(0xFF, 0xFF, 0xFF).toIntArray())
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ColorItem

        if (drawableRes != other.drawableRes) return false
        if (!color.contentEquals(other.color)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = drawableRes
        result = 31 * result + color.contentHashCode()
        return result
    }
}
