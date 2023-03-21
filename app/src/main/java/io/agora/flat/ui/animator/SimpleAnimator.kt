package io.agora.flat.ui.animator

import android.animation.ValueAnimator
import androidx.core.animation.addListener

/**
 * 显示过程中无法取消
 */
class SimpleAnimator(
    val onUpdate: (value: Float) -> Unit = {},
    val onShowStart: () -> Unit = {},
    val onShowEnd: () -> Unit = {},
    val onHideStart: () -> Unit = {},
    val onHideEnd: () -> Unit = {},
    private val duration: Long = 300,
) {
    private var shown = false
    private var animator: ValueAnimator? = null

    fun switch() {
        if (animator?.isRunning == true) {
            return
        }
        if (shown) {
            hide()
        } else {
            show()
        }
    }

    fun show() {
        animator = ValueAnimator.ofFloat(0F, 1F)
        animator?.apply {
            duration = this@SimpleAnimator.duration
            addUpdateListener {
                onUpdate(it.animatedValue as Float)
            }
            addListener(
                onEnd = {
                    onUpdate(1F)
                    shown = true
                    onShowEnd()
                },
                onStart = {
                    onUpdate(0F)
                    onShowStart()
                })
            start()
        }
    }

    fun hide() {
        animator = ValueAnimator.ofFloat(1F, 0F)
        animator?.apply {
            duration = this@SimpleAnimator.duration
            addUpdateListener {
                onUpdate(it.animatedValue as Float)
            }
            addListener(
                onEnd = {
                    onUpdate(0F)
                    shown = false
                    onHideEnd()
                },
                onStart = {
                    onUpdate(1F)
                    onHideStart()
                })
            start()
        }
    }

    fun isShown(): Boolean {
        return shown
    }
}