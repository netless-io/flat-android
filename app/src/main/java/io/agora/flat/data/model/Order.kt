package io.agora.flat.data.model

import com.google.gson.annotations.SerializedName

enum class Order {
    @SerializedName("ASC")
    Asc,

    @SerializedName("DESC")
    Desc,
}