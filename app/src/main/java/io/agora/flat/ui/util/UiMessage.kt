/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agora.flat.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import io.agora.flat.common.error.FlatErrorHandler
import io.agora.flat.util.showToast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*

enum class UiMessageType {
    Info,
    Error,
}

class UiMessage(
    val text: String = "",
    val exception: Throwable? = null,
    val type: UiMessageType = UiMessageType.Info,
    val id: Long = UUID.randomUUID().mostSignificantBits,
) {
    val isError: Boolean
        get() = type == UiMessageType.Error

    val isInfo: Boolean
        get() = type == UiMessageType.Info
}

fun UiInfoMessage(
    text: String,
): UiMessage = UiMessage(text = text, type = UiMessageType.Info)

fun UiErrorMessage(
    exception: Throwable,
): UiMessage = UiMessage(exception = exception, type = UiMessageType.Error)

class UiMessageManager {
    private val mutex = Mutex()

    private val _messages = MutableStateFlow(emptyList<UiMessage>())

    /**
     * A flow emitting the current message to display.
     */
    val message: Flow<UiMessage?> = _messages.map { it.firstOrNull() }.distinctUntilChanged()

    suspend fun emitMessage(message: UiMessage) {
        mutex.withLock {
            _messages.value = _messages.value + message
        }
    }

    suspend fun clearMessage(id: Long) {
        mutex.withLock {
            _messages.value = _messages.value.filterNot { it.id == id }
        }
    }
}

@Composable
fun ShowUiMessageEffect(
    uiMessage: UiMessage?,
    errorStr: (Throwable?) -> String? = { null },
    onMessageShown: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    uiMessage?.let { message ->
        LaunchedEffect(message) {
            val errorMessage = errorStr(message.exception)
                ?: FlatErrorHandler.getErrorStr(
                    context = context,
                    error = message.exception,
                    defaultValue = message.text
                )

            context.showToast(errorMessage)
            onMessageShown(message.id)
        }
    }
}