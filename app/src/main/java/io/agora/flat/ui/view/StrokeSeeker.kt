package io.agora.flat.ui.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

class StrokeSeeker @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    private var onStrokeChangedListener: OnStrokeChangedListener? = null

    private var indicatorPaint: Paint = Paint()
    private var indicatorWidth = 2.dp2px()
    private var indicatorHeight = 16.dp2px()

    private var offsetLeftX = 2.dp2px()
    private var offsetRightX = 8.dp2px()

    private var leftBarHeight = 2.dp2px()
    private val leftRect = RectF()
    private var leftPaint = Paint()

    private var rightBarHeight = 12.dp2px()
    private var rightRect = RectF()
    private var rightPaint = Paint()

    private var seekerPath = Path()

    private val leftLimit = offsetLeftX - leftBarHeight / 2
    private val rightLimit get() = width - offsetRightX + rightBarHeight / 2

    private var currentX = leftLimit
    private var baseY: Int = 0

    private var minStroke = 1
    private var maxStroke = 12
    private var currentStrokeWidth = -1

    init {
        indicatorPaint.isAntiAlias = true
        indicatorPaint.strokeWidth = indicatorWidth
        indicatorPaint.color = Color.parseColor("#3381FF")

        leftPaint.isAntiAlias = true
        leftPaint.color = Color.parseColor("#3381FF")

        rightPaint.isAntiAlias = true
        rightPaint.color = Color.parseColor("#F3F6F9")
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        baseY = height / 2
        leftRect.set(
            offsetLeftX - leftBarHeight,
            baseY - leftBarHeight / 2,
            offsetLeftX + leftBarHeight / 2,
            baseY + leftBarHeight / 2
        )

        rightRect.set(
            width - offsetRightX - rightBarHeight / 2,
            baseY - rightBarHeight / 2,
            width - offsetRightX + rightBarHeight / 2,
            baseY + rightBarHeight / 2
        )

        seekerPath.reset()
        seekerPath.moveTo(offsetLeftX, baseY - leftBarHeight / 2)
        seekerPath.lineTo(width - offsetRightX, baseY - rightBarHeight / 2)
        seekerPath.arcTo(rightRect, 270f, 180f)
        seekerPath.lineTo(offsetLeftX, baseY + leftBarHeight / 2)
        seekerPath.arcTo(leftRect, 90f, 180f)

        setStrokeWidth(currentStrokeWidth)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.save()
        canvas.clipRect(currentX, 0f, width.toFloat(), height.toFloat())
        canvas.drawPath(seekerPath, rightPaint)
        canvas.restore()

        canvas.save()
        canvas.clipRect(0f, 0f, currentX, height.toFloat())
        canvas.drawPath(seekerPath, leftPaint)
        canvas.restore()

        canvas.drawLine(
            currentX,
            baseY - indicatorHeight / 2,
            currentX,
            baseY + indicatorHeight / 2,
            indicatorPaint,
        )
    }

    private fun currentBarHeight(): Float {
        val wa = width - offsetLeftX - offsetRightX
        val wp = currentX - offsetLeftX

        return (rightBarHeight - leftBarHeight) * wp / wa + leftBarHeight
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                updateCurrentX(event.x)
            }
            MotionEvent.ACTION_MOVE -> {
                updateCurrentX(event.x)
            }
            MotionEvent.ACTION_UP -> {
            }
            MotionEvent.ACTION_CANCEL -> {
            }
        }
        return true
    }

    private fun updateCurrentX(x: Float) {
        currentX = x.coerceIn(leftLimit, rightLimit)
        invalidate()

        val per = (currentX - leftLimit) / (rightLimit - leftLimit)
        val result = (rangeSize() * per + minStroke).coerceAtMost(maxStroke.toFloat()).toInt()
        if (result != currentStrokeWidth) {
            currentStrokeWidth = result
            onStrokeChangedListener?.onStroke(result)
        }
    }

    private fun rangeSize() = maxStroke - minStroke + 1

    private fun Int.dp2px(): Float = toFloat().dp2px()

    private fun Float.dp2px(): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, this, context.resources.displayMetrics
    )

    fun setStrokeRange(minStroke: Int, maxStroke: Int) {
        this.minStroke = minStroke
        this.maxStroke = maxStroke
    }

    private fun setStrokeWidth(strokeWidth: Int) {
        currentStrokeWidth = strokeWidth
        currentX = (strokeWidth - minStroke).toFloat() / rangeSize() * (rightLimit - leftLimit) + leftLimit
        invalidate()
    }

    fun setOnStrokeChangedListener(onStrokeChangedListener: OnStrokeChangedListener) {
        this.onStrokeChangedListener = onStrokeChangedListener
    }

    fun interface OnStrokeChangedListener {
        fun onStroke(width: Int)
    }
}