package io.agora.flat.ui.activity.play

import com.herewhite.sdk.domain.Appliance
import io.agora.flat.R

enum class ApplianceItem(val drawableRes: Int, val type: String) {
    CLICKER(R.drawable.ic_toolbox_clicker_selector, Appliance.CLICKER),
    SELECTOR(R.drawable.ic_toolbox_selector_selector, Appliance.SELECTOR),
    PENCIL(R.drawable.ic_toolbox_pencil_selector, Appliance.PENCIL),
    RECTANGLE(R.drawable.ic_toolbox_rectangle_selector, Appliance.RECTANGLE),
    ELLIPSE(R.drawable.ic_toolbox_circle_selector, Appliance.ELLIPSE),
    TEXT(R.drawable.ic_toolbox_text_selector, Appliance.TEXT),
    ERASER(R.drawable.ic_toolbox_eraser_selector, Appliance.ERASER),
    LASER_POINTER(R.drawable.ic_toolbox_laser_selector, Appliance.LASER_POINTER),
    ARROW(R.drawable.ic_toolbox_arrow_selector, Appliance.ARROW),
    STRAIGHT(R.drawable.ic_toolbox_line_selector, Appliance.STRAIGHT),
    HAND(R.drawable.ic_toolbox_hand_selector, Appliance.HAND),

    OTHER_CLEAR(R.drawable.ic_toolbox_clear_normal, ""),
    ;

    companion object {
        var appliancesPhone = listOf(
            SELECTOR,
            PENCIL,
            RECTANGLE,
            TEXT,
            ERASER,
            HAND,
            OTHER_CLEAR
        )

        fun drawableResOf(type: String): Int {
            return values().find { it.type == type }?.drawableRes ?: 0
        }

        fun of(type: String): ApplianceItem {
            return values().find { it.type == type } ?: CLICKER
        }
    }
}