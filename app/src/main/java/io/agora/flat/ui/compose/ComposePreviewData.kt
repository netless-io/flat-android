package io.agora.flat.ui.compose

import com.google.gson.Gson
import io.agora.flat.data.model.RoomInfo

object ComposePreviewData {
    val roomInfo: RoomInfo
        get() {
            val roomStr = "{\n" +
                    "            \"roomUUID\": \"c97348d5-87e4-4154-8e10-c939ed9cb041\",\n" +
                    "            \"periodicUUID\": null,\n" +
                    "            \"ownerUUID\": \"722f7f6d-cc0f-4e63-a543-446a3b7bd659\",\n" +
                    "            \"roomType\": \"OneToOne\",\n" +
                    "            \"title\": \"XXX创建的房间\",\n" +
                    "            \"ownerAvatarURL\":\"https://flat-storage.oss-accelerate.aliyuncs.com/cloud-storage/avatar/3c806abd-4e53-4621-b42f-549209809f08/834ef7e5-e61d-4333-85bb-b256106ad114.jpeg\",\n" +
                    "            \"beginTime\": 1615371918318,\n" +
                    "            \"endTime\": 1615391955296,\n" +
                    "            \"roomStatus\": \"Idle\",\n" +
                    "            \"ownerName\": \"XXX\",\n" +
                    "            \"inviteCode\": \"c97348d5\",\n" +
                    "            \"region\": \"cn-hz\",\n" +
                    "            \"hasRecord\": true\n" +
                    "        }"
            return Gson().fromJson(roomStr, RoomInfo::class.java)
        }
}