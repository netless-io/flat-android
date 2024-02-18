package io.agora.flat.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import io.agora.flat.R
import io.agora.flat.databinding.LayoutTimeStateBinding
import java.util.concurrent.TimeUnit


data class TimeStateData(
    val begin: Long,
    val end: Long,
    val endNotify: Long,
)

class TimeStateLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {
    private var binding: LayoutTimeStateBinding = LayoutTimeStateBinding.inflate(
        LayoutInflater.from(context),
        this,
    )
    private var timeStateData: TimeStateData = TimeStateData(0, Long.MAX_VALUE, 5 * 60 * 1000)

    private val ticker: Runnable = object : Runnable {
        override fun run() {
            removeCallbacks(this)
            updateView()
            postDelayed(this, 1000)
        }
    }

    fun updateTimeStateData(data: TimeStateData) {
        timeStateData = data
        ticker.run()
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        if (isVisible) {
            ticker.run()
        } else {
            removeCallbacks(ticker)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(ticker)
    }

    private fun updateView() {
        val current = System.currentTimeMillis()
        val begin = timeStateData.begin
        val end = timeStateData.end
        val endNotify = timeStateData.endNotify

        val timeStateTextView: TextView = binding.timeState

        if (current < begin) {
            val formattedWaitingTime = formatMillisToTime(begin - current)
            val waitingText = context.getString(R.string.room_class_time_waiting_format, formattedWaitingTime)

            timeStateTextView.text = waitingText
            timeStateTextView.setTextColorRes(R.color.flat_gray)
        } else if (current < end) {
            val leftTime = end - current

            val timeText = if (leftTime <= endNotify) {
                context.getString(R.string.room_class_time_left_format, formatMillisToTime(end - current))
            } else {
                context.getString(R.string.room_class_time_started_format, formatMillisToTime(current - begin))
            }

            val colorResId = if (leftTime <= endNotify) R.color.flat_yellow else R.color.flat_light_green

            timeStateTextView.text = timeText
            timeStateTextView.setTextColorRes(colorResId)
        } else {
            timeStateTextView.text = context.getString(R.string.room_class_time_ended)
        }
    }

    private fun formatMillisToTime(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun TextView.setTextColorRes(@ColorRes colorResId: Int) {
        setTextColor(ContextCompat.getColor(context, colorResId))
    }
}