package io.agora.flat.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberAndroidClipboardController(
    context: Context = LocalContext.current
): AndroidClipboardController = remember(context) {
    AndroidClipboardController(context)
}

class AndroidClipboardController(context: Context) {
    private val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    fun putText(text: CharSequence) {
        val clip: ClipData = ClipData.newPlainText("FlatInvite", text)
        clipboard.setPrimaryClip(clip)
    }

    fun getText(): CharSequence {
        if (!clipboard.hasPrimaryClip())
            return ""
        return clipboard.primaryClip?.getItemAt(0)?.text ?: ""
    }
}
