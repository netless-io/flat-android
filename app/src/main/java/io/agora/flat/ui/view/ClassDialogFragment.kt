package io.agora.flat.ui.view

import android.view.Window
import android.view.WindowManager
import androidx.annotation.LayoutRes
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.DialogFragment

open class ClassDialogFragment constructor(
    @LayoutRes contentLayoutId: Int,
) : DialogFragment(contentLayoutId) {

    override fun onStart() {
        markNotFocusable()
        super.onStart()
        unmarkNotFocusable()
        hideBars(dialog?.window)
    }

    private fun markNotFocusable() {
        dialog?.window?.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        )
    }

    private fun unmarkNotFocusable() {
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
    }

    private fun hideBars(window: Window?) {
        if (window != null) {
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.navigationBars())
            controller.hide(WindowInsetsCompat.Type.statusBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}