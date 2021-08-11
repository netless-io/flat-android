package io.agora.flat.data.model

data class RoomCreateReq constructor(
    // 房间主题, 最多 50 字
    val title: String,
    // 上课类型
    val type: RoomType,
    // 单位ms
    val beginTime: Long,
    // 如果不传，则默认是 beginTime 后的一个小时)
    val endTime: Long? = beginTime + 3600_000,
    // 区域标记
    val region: String = "cn-hz",
)