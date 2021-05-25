package io.agora.flat.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay

/**
 * 存在动画的交互请求，有助于视觉体验的提升
 * 例如：点击 loading 后跳转
 */
suspend fun <T> CoroutineScope.runAtLeast(time: Long = 2000, block: suspend () -> T): T {
    val start = System.currentTimeMillis()
    val result = block()
    if (System.currentTimeMillis() - start < time) {
        delay(time - (System.currentTimeMillis() - start))
    }
    return result;
}