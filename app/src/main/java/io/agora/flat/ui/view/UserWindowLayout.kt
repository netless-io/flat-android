package io.agora.flat.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.*
import android.widget.FrameLayout
import androidx.core.view.isVisible
import coil.load
import coil.transform.CircleCropTransformation
import io.agora.flat.data.model.RoomUser
import io.agora.flat.databinding.LayoutUserWindowBinding
import kotlin.math.abs

class UserWindowLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {
    private var binding: LayoutUserWindowBinding = LayoutUserWindowBinding.inflate(
        LayoutInflater.from(context),
        this,
    )
    private var scaledTouchSlop: Int

    private var doScale: Boolean = false
    private var doMove: Boolean = false
    private val dismissLayout = Runnable {
        binding.switchDeviceLayout.isVisible = false
    }


    init {
        scaledTouchSlop = ViewConfiguration.get(context).scaledTouchSlop

        binding.switchDeviceLayout.isVisible = false
        binding.switchCamera.setOnClickListener {
            onUserWindowListener?.onSwitchCamera(roomUser, !binding.switchCamera.isSelected)
        }
        binding.switchCamera.setImageDrawable(FlatDrawables.createCameraDrawable(context))

        binding.switchMic.setOnClickListener {
            onUserWindowListener?.onSwitchMic(roomUser, !binding.switchMic.isSelected)
        }
        binding.switchMic.setImageDrawable(FlatDrawables.createMicDrawable(context))

        val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                doScale = true
                onWindowDragListener?.onWindowScale(roomUser.userUUID, detector.scaleFactor)
                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {
                onWindowDragListener?.onWindowScaleEnd(roomUser.userUUID)
            }
        })

        val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                return onWindowDragListener?.onDoubleTap(roomUser.userUUID) ?: false
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                onUserWindowListener?.onUserWindowClick(this@UserWindowLayout)
                return true
            }
        })

        setOnTouchListener(object : OnTouchListener {
            var mLastX: Float = 0f
            var mLastY: Float = 0f
            var mActivePointerId: Int = 0

            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                scaleDetector.onTouchEvent(event)
                gestureDetector.onTouchEvent(event)

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        doMove = false
                        doScale = false

                        mActivePointerId = event.getPointerId(0)
                        mLastX = event.rawX
                        mLastY = event.rawY

                        onWindowDragListener?.onActionStart(roomUser.userUUID)
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (doScale) return false
                        if (event.getPointerId(event.actionIndex) != mActivePointerId) return false
                        val dx: Float = event.rawX - mLastX
                        val dy: Float = event.rawY - mLastY
                        if (!doMove && abs(dx) < scaledTouchSlop && abs(dy) < scaledTouchSlop) return false
                        doMove = true

                        mLastX = event.rawX
                        mLastY = event.rawY

                        onWindowDragListener?.onWindowMove(roomUser.userUUID, dx, dy)
                    }

                    MotionEvent.ACTION_UP -> {
                        if (event.getPointerId(event.actionIndex) == mActivePointerId && doMove) {
                            onWindowDragListener?.onWindowMoveEnd(roomUser.userUUID)
                        }
                    }
                }
                return true
            }
        })
    }

    private lateinit var roomUser: RoomUser

    fun getContainer(): FrameLayout = binding.videoContainer

    fun setRoomUser(user: RoomUser) {
        roomUser = user
        binding.avatar.load(roomUser.avatarURL) {
            crossfade(true)
            transformations(CircleCropTransformation())
        }
        binding.teacherLeaveLy.isVisible = roomUser.isLeft && roomUser.isOwner
        binding.studentLeaveLy.isVisible = roomUser.isLeft && !roomUser.isOwner
        binding.micClosed.isVisible = !roomUser.audioOpen
        binding.username.text = roomUser.name
        binding.videoClosedLayout.isVisible = roomUser.isJoined && !roomUser.videoOpen
        binding.switchCamera.isSelected = roomUser.videoOpen
        binding.switchMic.isSelected = roomUser.audioOpen
        invalidate()
    }

    fun getUserUUID(): String = roomUser.userUUID

    private var onUserWindowListener: OnUserWindowListener? = null
    private var onWindowDragListener: OnWindowDragListener? = null

    fun setUserWindowListener(listener: OnUserWindowListener) {
        onUserWindowListener = listener
    }

    fun setOnWindowDragListener(listener: OnWindowDragListener) {
        onWindowDragListener = listener
    }

    fun showDeviceControl() {
        binding.switchDeviceLayout.isVisible = true
        binding.switchDeviceLayout.removeCallbacks(dismissLayout)
        binding.switchDeviceLayout.postDelayed(dismissLayout, 3000)
    }

    interface OnUserWindowListener {
        fun onUserWindowClick(userWindowLayout: UserWindowLayout) {}

        fun onSwitchCamera(user: RoomUser, on: Boolean) {}

        fun onSwitchMic(user: RoomUser, on: Boolean) {}
    }

    interface OnWindowDragListener {
        fun onActionStart(uuid: String) {}

        fun onWindowScale(uuid: String, scale: Float)

        fun onWindowScaleEnd(uuid: String)

        fun onWindowMove(uuid: String, dx: Float, dy: Float)

        fun onWindowMoveEnd(uuid: String) {

        }

        fun onDoubleTap(userId: String): Boolean {
            return false
        }
    }
}