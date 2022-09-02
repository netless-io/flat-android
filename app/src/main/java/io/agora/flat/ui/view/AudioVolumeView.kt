package io.agora.flat.ui.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.graphics.PathParser
import kotlin.random.Random

class AudioVolumeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private var volumeProgress: Float = 0.6f
    private var paint: Paint = Paint()
    private var paintWave: Paint = Paint()
    private var paintBg: Paint = Paint()
    private var micPath1: Path
    private var micPath2: Path

    // bottom 18f -> 4f
    private var rectWave = RectF(8f, 0f, 16f, 18f)
    private var random = Random

    private var sx = 1f
    private var sy = 1f

    init {
        paint.isAntiAlias = true
        paint.color = Color.parseColor("#FFFFFF")
        paint.strokeWidth = 2f
        paintWave.isAntiAlias = true
        paintWave.color = Color.parseColor("#44AD00")
        paintBg.isAntiAlias = true
        paintBg.color = Color.parseColor("#7F999CA3")
        paintBg.style = Paint.Style.FILL

        micPath1 = PathParser.createPathFromPathData(
            "M12,4L12,4A4,4 0,0 1,16 8L16,14A4,4 0,0 1,12 18" +
                    "L12,18A4,4 0,0 1,8 14L8,8A4,4 0,0 1,12 4z"
        )

        micPath2 = PathParser.createPathFromPathData(
            "M4,16.625H6V15.375H4V16.625ZM10,20.625H14V19.375H10V20.625ZM18,16.625" +
                    "H20V15.375H18V16.625ZM14,20.625C16.554,20.625 18.625,18.554 18.625,16" +
                    "H17.375C17.375,17.864 15.864,19.375 14,19.375V20.625Z" +
                    "M5.375,16C5.375,18.554 7.446,20.625 10,20.625V19.375C8.136,19.375 6.625,17.864 6.625,16H5.375Z"
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        sx = width / 24.toFloat()
        sy = height / 24.toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.save()
        canvas.drawCircle(width.toFloat() / 2, height.toFloat() / 2, width.toFloat() / 2, paintBg)
        canvas.restore()

        canvas.save()
        canvas.scale(sx, sy)
        paint.style = Paint.Style.FILL
        canvas.drawPath(micPath1, paint)
        paint.style = Paint.Style.STROKE
        canvas.drawPath(micPath2, paint)
        canvas.restore()

        canvas.save()
        canvas.scale(sx, sy)
        canvas.clipPath(micPath1)
        rectWave.top = computeWaveTop()
        canvas.drawRect(rectWave, paintWave)
        canvas.restore()

        postDelayed({ invalidate() }, 50)
    }

    private fun computeWaveTop(): Float {
        val value = 18f + (4 - 18) * volumeProgress
        val valueRandom = value + random.nextFloat() * 2 - 1
        return valueRandom.coerceIn(4f, 18f)
    }

    private fun updateVolume(progress: Float) {
        this.volumeProgress = progress
    }

    private fun Int.dp2px(): Float = toFloat().dp2px()

    private fun Float.dp2px(): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, this, context.resources.displayMetrics
    )
}