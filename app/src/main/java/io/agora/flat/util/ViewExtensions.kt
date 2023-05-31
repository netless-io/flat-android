package io.agora.flat.util

import android.graphics.Rect
import android.os.Build
import android.view.DisplayCutout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.LayoutRes
import androidx.annotation.RequiresApi
import coil.imageLoader
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation

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

fun ImageView.loadAvatarAny(data: Any) {
    val request = ImageRequest.Builder(context)
        .data(data)
        .target(this)
        .apply {
            crossfade(true)
            transformations(CircleCropTransformation())
        }
        .build()
    context.imageLoader.enqueue(request)
}

fun View.getViewRect(anchorView: View): Rect {
    val array = IntArray(2)
    getLocationOnScreen(array)

    val arrayP = IntArray(2)
    anchorView.getLocationOnScreen(arrayP)

    return Rect(
        array[0] - arrayP[0],
        array[1] - arrayP[1],
        array[0] - arrayP[0] + this.width,
        array[1] - arrayP[1] + this.height
    )
}

const val ANIMATION_FAST_MILLIS = 50L
const val ANIMATION_SLOW_MILLIS = 100L

/**
 * Simulate a button click, including a small delay while it is being pressed to trigger the
 * appropriate animations.
 */
fun View.simulateClick(delay: Long = ANIMATION_FAST_MILLIS) {
    performClick()
    isPressed = true
    invalidate()
    postDelayed({
        invalidate()
        isPressed = false
    }, delay)
}

/** Pad this view with the insets provided by the device cutout (i.e. notch) */
@RequiresApi(Build.VERSION_CODES.P)
fun View.padWithDisplayCutout() {

    /** Helper method that applies padding from cutout's safe insets */
    fun doPadding(cutout: DisplayCutout) = setPadding(
        cutout.safeInsetLeft,
        cutout.safeInsetTop,
        cutout.safeInsetRight,
        cutout.safeInsetBottom
    )

    // Apply padding using the display cutout designated "safe area"
    rootWindowInsets?.displayCutout?.let { doPadding(it) }

    // Set a listener for window insets since view.rootWindowInsets may not be ready yet
    setOnApplyWindowInsetsListener { _, insets ->
        insets.displayCutout?.let { doPadding(it) }
        insets
    }
}
