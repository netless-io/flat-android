package io.agora.flat.ui.manager

import io.agora.flat.common.board.WindowInfo
import org.junit.Assert
import org.junit.Test
import kotlin.math.abs

class WindowsDragManagerTest {

    companion object {
        val BASE_TEST = arrayOf(
            arrayOf(WindowInfo(0f, 0f, 0, 1f, 1f)),
            arrayOf(
                WindowInfo(0.0f, 0.25f, 0, 0.5f, 0.5f),
                WindowInfo(0.5f, 0.25f, 0, 0.5f, 0.5f),
            ),
            arrayOf(
                WindowInfo(0.0f, 0f, 0, 0.5f, 0.5f),
                WindowInfo(0.5f, 0f, 0, 0.5f, 0.5f),
                WindowInfo(0.25f, 0.5f, 0, 0.5f, 0.5f),
            ),
            arrayOf(
                WindowInfo(0f, 0f, 0, 0.5f, 0.5f),
                WindowInfo(0.5f, 0f, 0, 0.5f, 0.5f),
                WindowInfo(0f, 0.5f, 0, 0.5f, 0.5f),
                WindowInfo(0.5f, 0.5f, 0, 0.5f, 0.5f),
            )
        )

        fun generateWindowLayoutJson(): String {
            val buffer = StringBuffer()
            buffer.append("[")
            for (size in 1..16) {
                val windows = WindowsDragManager.getMaximizeWindowsInfo(size)
                buffer.append("[")
                for (index in windows.indices) {
                    buffer.append(stringWindowInfo(windows[index]))
                    if (index != windows.indices.last) buffer.append(",")
                }
                buffer.append("]")
                if (size != 16) buffer.append(",")
            }
            buffer.append("]")
            return buffer.toString()
        }

        private fun equalWindowInfo(w1: WindowInfo, w2: WindowInfo): Boolean {
            return abs(w1.x - w2.x) < 0.01f &&
                    abs(w1.y - w2.y) < 0.01f &&
                    abs(w1.width - w2.width) < 0.01f &&
                    abs(w1.height - w2.height) < 0.01f
        }

        private fun stringWindowInfo(w: WindowInfo): String {
            return with(w) {
                "{\"x\":$x, \"y\":$y, \"z\":$z, \"width\":$width, \"height\":$height}"
            }
        }
    }

    @Test
    fun testGetMaximizeWindowsInfo() {
        for (size in 1..4) {
            val windows = WindowsDragManager.getMaximizeWindowsInfo(size)
            val expect = BASE_TEST[size - 1]
            for (i in expect.indices) {
                if (!equalWindowInfo(windows[i], expect[i])) {
                    Assert.fail("size: $size, index: $i, expect: ${expect[i]}, actual: ${windows[i]}")
                    return
                }
            }
        }
    }

    @Test
    fun testGetMaximizeWindowsInfo_ShouldEmptyWhenSizeIsZero() {
        val windows = WindowsDragManager.getMaximizeWindowsInfo(0)
        Assert.assertEquals(0, windows.size)
    }
}