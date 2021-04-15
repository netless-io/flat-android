package io.agora.flat.data.model

import com.google.gson.annotations.SerializedName

enum class Week {
    @SerializedName("0")
    Sunday,

    @SerializedName("1")
    Monday,

    @SerializedName("2")
    Tuesday,

    @SerializedName("3")
    Wednesday,

    @SerializedName("4")
    Thursday,

    @SerializedName("5")
    Friday,

    @SerializedName("6")
    Saturday,
}