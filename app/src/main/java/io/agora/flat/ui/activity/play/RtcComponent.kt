package io.agora.flat.ui.activity.play

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.agora.flat.common.EventHandler
import io.agora.flat.ui.viewmodel.ClassRoomViewModel
import io.agora.flat.util.showToast
import io.agora.rtc.IRtcEngineEventHandler
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class RtcComponent(
    activity: ClassRoomActivity,
    rootView: FrameLayout,
) : BaseComponent(activity, rootView) {
    companion object {
        val TAG = RtcComponent::class.simpleName
    }

    private val REQUESTED_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private var viewModel: ClassRoomViewModel =
        ViewModelProvider(activity).get(ClassRoomViewModel::class.java)
    private lateinit var recyclerView: RecyclerView
    private var adpater: UserVideoAdapter = UserVideoAdapter(ArrayList(), application().rtcEngine())

    override fun onCreate(owner: LifecycleOwner) {
        initView()
        // loadData()
        // updateView()
        initListener()
        checkPermission(::actionAfterPermission)
    }

    private fun loadData() {
        lifecycleScope.launch {
            viewModel.roomUsersMap.collect {
                it.map { entry ->
                    Log.d(TAG, "${entry.key} ${entry.value.name}")
                }
                adpater.setDataSet(ArrayList(it.values))
            }
        }
    }

    private fun initView() {
        recyclerView = RecyclerView(activity)
        rootView.addView(recyclerView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adpater
    }

    override fun onDestroy(owner: LifecycleOwner) {
        application().rtcEngine()?.leaveChannel()
    }

    private fun checkPermission(actionAfterPermission: () -> Unit) {
        val permissions = REQUESTED_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(
                activity,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissions.isEmpty()) {
            actionAfterPermission()
        }

        Log.i(TAG, "requestPermissions $permissions")
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { it ->
            val allGranted = it.mapNotNull { it.key }.size == it.size
            if (allGranted) {
                actionAfterPermission()
            } else {
                activity.showToast("Permission Not Granted")
            }
        }.launch(permissions)
    }

    private fun actionAfterPermission() {
        loadData()
    }

    private fun initListener() {
        application().registerEventHandler(object : EventHandler {
            override fun onFirstRemoteVideoDecoded(
                uid: Int,
                width: Int,
                height: Int,
                elapsed: Int
            ) {
                Log.d(TAG, "onFirstRemoteVideoDecoded")
            }

            override fun onLeaveChannel(stats: IRtcEngineEventHandler.RtcStats?) {
                Log.d(TAG, "onLeaveChannel")
            }

            override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                Log.d(TAG, "onJoinChannelSuccess:$channel\t$uid\t$elapsed")
                adpater.notifyDataSetChanged()
            }

            override fun onUserOffline(uid: Int, reason: Int) {
                Log.d(TAG, "onUserOffline:$uid $reason")
            }

            override fun onUserJoined(uid: Int, elapsed: Int) {
                Log.d(TAG, "onUserJoined:$uid $elapsed")
            }
        })
    }
}
