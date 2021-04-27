package io.agora.flat.ui.activity.play

import androidx.annotation.DrawableRes
import io.agora.flat.R

data class ColorItem(val color: IntArray, @DrawableRes val drawableRes: Int) {
    companion object {
        var colors = listOf(
            ColorItem(arrayOf(0xE0, 0x20, 0x20).toIntArray(), R.drawable.ic_toolbox_color_red),
            ColorItem(arrayOf(0xFA, 0x64, 0x00).toIntArray(), R.drawable.ic_toolbox_color_orange),
            ColorItem(arrayOf(0xF7, 0xB5, 0x00).toIntArray(), R.drawable.ic_toolbox_color_yellow),
            ColorItem(arrayOf(0x6D, 0xD4, 0x00).toIntArray(), R.drawable.ic_toolbox_color_green),
            ColorItem(arrayOf(0x44, 0xD7, 0xB6).toIntArray(), R.drawable.ic_toolbox_color_cyan),
            ColorItem(arrayOf(0x00, 0x91, 0xFF).toIntArray(), R.drawable.ic_toolbox_color_blue),
            ColorItem(arrayOf(0x62, 0x36, 0xFF).toIntArray(), R.drawable.ic_toolbox_color_deep_purple,),
            ColorItem(arrayOf(0xB6, 0x20, 0xE0).toIntArray(), R.drawable.ic_toolbox_color_purple),
            ColorItem(arrayOf(0xBC, 0xC0, 0xC6).toIntArray(), R.drawable.ic_toolbox_color_blue_gray),
            ColorItem(arrayOf(0x6D, 0x72, 0x78).toIntArray(), R.drawable.ic_toolbox_color_gray),
            ColorItem(arrayOf(0x00, 0x00, 0x00).toIntArray(), R.drawable.ic_toolbox_color_black),
            ColorItem(arrayOf(0xFF, 0xFF, 0xFF).toIntArray(), R.drawable.ic_toolbox_color_white)
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
