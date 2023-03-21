package io.agora.flat.ui.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import androidx.core.content.ContextCompat
import io.agora.flat.R

// @formatter:off
object FlatDrawables {
    fun createCameraDrawable(context: Context): Drawable {
        val cameraDrawable = StateListDrawable()
        val cameraOn = context.getDrawable(R.drawable.ic_class_room_camera_on)
        val cameraOnDisable = context.getDrawable(R.drawable.ic_class_room_camera_on)?.apply {
            setTint(ContextCompat.getColor(context, R.color.flat_day_night_text_secondary))
        }
        val cameraOff = context.getDrawable(R.drawable.ic_class_room_camera_off)
        val cameraOffDisable = context.getDrawable(R.drawable.ic_class_room_camera_off)?.apply {
            setTint(ContextCompat.getColor(context, R.color.flat_day_night_text_secondary))
        }
        cameraDrawable.addState(intArrayOf(android.R.attr.state_enabled, android.R.attr.state_selected), cameraOn)
        cameraDrawable.addState(intArrayOf(-android.R.attr.state_enabled, android.R.attr.state_selected), cameraOnDisable)
        cameraDrawable.addState(intArrayOf(android.R.attr.state_enabled, -android.R.attr.state_selected), cameraOff)
        cameraDrawable.addState(intArrayOf(-android.R.attr.state_enabled, -android.R.attr.state_selected), cameraOffDisable)
        cameraDrawable.addState(intArrayOf(), cameraOffDisable)

        return cameraDrawable
    }

    fun createMicDrawable(context: Context): Drawable {
        val micDrawable = StateListDrawable()
        val micOn = context.getDrawable(R.drawable.ic_class_room_mic_on)
        val micOnDisable = context.getDrawable(R.drawable.ic_class_room_mic_on)?.apply {
            setTint(ContextCompat.getColor(context, R.color.flat_day_night_text_secondary))
        }
        val micOff = context.getDrawable(R.drawable.ic_class_room_mic_off)
        val micOffDisable = context.getDrawable(R.drawable.ic_class_room_mic_off)?.apply {
            setTint(ContextCompat.getColor(context, R.color.flat_day_night_text_secondary))
        }

        micDrawable.addState(intArrayOf(android.R.attr.state_enabled, android.R.attr.state_selected), micOn)
        micDrawable.addState(intArrayOf(-android.R.attr.state_enabled, android.R.attr.state_selected), micOnDisable)
        micDrawable.addState(intArrayOf(android.R.attr.state_enabled, -android.R.attr.state_selected), micOff)
        micDrawable.addState(intArrayOf(-android.R.attr.state_enabled, -android.R.attr.state_selected), micOffDisable)
        micDrawable.addState(intArrayOf(), micOffDisable)

        return micDrawable
    }
}
// @formatter:on