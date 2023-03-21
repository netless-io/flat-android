package io.agora.flat.ui.activity.play

import android.graphics.Rect

data class UserWindowUiState(
    val centerX: Float,
    val centerY: Float,
    val width: Float,
    val height: Float,
    val index: Int,
) {
    private var rect: Rect? = null

    fun getRect(): Rect {
        if (rect == null) rect = Rect()
        val rect = rect!!
        rect.set(
            (centerX - width / 2).toInt(),
            (centerY - height / 2).toInt(),
            (centerX + width / 2).toInt(),
            (centerY + height / 2).toInt(),
        )
        return rect
    }
}