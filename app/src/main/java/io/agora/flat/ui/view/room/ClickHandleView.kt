package io.agora.flat.ui.view.room

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.core.view.isVisible

/**
 * TODO Handle Hide Call Out
 */
class ClickHandleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    fun show(shown: Boolean, listener: Listener) {
        this.isVisible = shown
        if (shown) {
            this.setOnClickListener {
                this.isVisible = false
                listener.onClearAll()
            }
        }
    }

    fun interface Listener {
        fun onClearAll()
    }
}