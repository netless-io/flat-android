package io.agora.flat.util

import android.app.Activity
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.WindowManager.LayoutParams
import android.widget.PopupWindow

class KeyboardHeightProvider(private val activity: Activity) : PopupWindow(activity), OnGlobalLayoutListener {
    private val rootView: View = View(activity)
    private var listener: HeightListener? = null
    private var heightMax = 0

    init {
        contentView = rootView

        rootView.viewTreeObserver.addOnGlobalLayoutListener(this)
        setBackgroundDrawable(ColorDrawable(0))

        width = 0
        height = LayoutParams.MATCH_PARENT

        softInputMode = LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        inputMethodMode = INPUT_METHOD_NEEDED
    }

    fun start() {
        if (!isShowing) {
            val view: View = activity.window.decorView
            view.post { showAtLocation(view, Gravity.NO_GRAVITY, 0, 0) }
        }
    }

    fun stop() {
        if (isShowing) {
            dismiss()
        }
    }

    fun setHeightListener(listener: HeightListener?): KeyboardHeightProvider {
        this.listener = listener
        return this
    }

    override fun onGlobalLayout() {
        val rect = Rect()
        rootView.getWindowVisibleDisplayFrame(rect)
        if (rect.bottom > heightMax) {
            heightMax = rect.bottom
        }

        // 两者的差值就是键盘的高度
        val keyboardHeight: Int = heightMax - rect.bottom
        if (listener != null) {
            listener!!.onHeightChanged(keyboardHeight)
        }
    }

    interface HeightListener {
        fun onHeightChanged(height: Int)
    }
}