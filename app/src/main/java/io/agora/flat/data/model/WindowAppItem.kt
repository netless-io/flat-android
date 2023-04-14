package io.agora.flat.data.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.herewhite.sdk.domain.WindowAppParam
import io.agora.flat.R

data class WindowAppItem(
    @DrawableRes val icon: Int,
    @StringRes val title: Int,
    val kind: String,
) {
    companion object {
        val apps = listOf(
            WindowAppItem(R.drawable.ic_apps_code, R.string.app_name_code, "Monaco"),
            WindowAppItem(R.drawable.ic_apps_geometry, R.string.app_name_geometry, "GeoGebra"),
            WindowAppItem(R.drawable.ic_apps_clock, R.string.app_name_clock, "Countdown"),
            WindowAppItem(R.drawable.ic_apps_question, R.string.app_name_question, "Selector"),
            WindowAppItem(R.drawable.ic_apps_dice, R.string.app_name_dice, "Dice"),
            WindowAppItem(R.drawable.ic_apps_rich_text, R.string.app_name_rich_text, "Quill"),
            WindowAppItem(R.drawable.ic_apps_mindmap, R.string.app_name_mindmap, "MindMap"),
        )
    }
}