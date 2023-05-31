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

package io.agora.flat.data

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

sealed class Result<T> {
    open fun get(): T? = null

    fun getOrThrow(): T = when (this) {
        is Success -> get()
        is Failure -> throw exception
    }

    fun asFailure() = this as Failure

    val isSuccess: Boolean get() = this !is Failure

    val isFailure: Boolean get() = this is Failure
}

data class Success<T>(val data: T) : Result<T>() {
    override fun get(): T = data
}

data class Failure<T>(val exception: Throwable) : Result<T>()

@OptIn(ExperimentalContracts::class)
inline fun <T> Result<T>.onSuccess(action: (value: T) -> Unit): Result<T> {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    if (isSuccess) action(get() as T)
    return this
}

@OptIn(ExperimentalContracts::class)
inline fun <T> Result<T>.onFailure(action: (exception: Throwable) -> Unit): Result<T> {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    if (isFailure) action(asFailure().exception)
    return this
}
