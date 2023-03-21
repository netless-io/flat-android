package io.agora.flat.util

import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.LayoutRes

fun ViewGroup.inflate(@LayoutRes resource: Int, root: ViewGroup, attachToRoot: Boolean): View {
    return LayoutInflater.from(context).inflate(resource, root, attachToRoot)
}

fun View.renderTo(rect: Rect) {
    val layoutParams = layoutParams as FrameLayout.LayoutParams
    layoutParams.width = rect.width()
    layoutParams.height = rect.height()
    layoutParams.leftMargin = rect.left
    layoutParams.topMargin = rect.top
    this.layoutParams = layoutParams
}