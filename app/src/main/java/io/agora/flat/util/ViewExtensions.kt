package io.agora.flat.util

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.annotation.Nullable

fun ViewGroup.inflate(@LayoutRes resource: Int, @Nullable root: ViewGroup, attachToRoot: Boolean): View {
    return LayoutInflater.from(context).inflate(resource, root, attachToRoot)
}