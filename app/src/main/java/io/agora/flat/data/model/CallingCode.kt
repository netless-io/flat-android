package io.agora.flat.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Country(
    // country name
    val name: String,
    // calling code
    val cc: String,
    // country code
    val code: String,
) : Parcelable
