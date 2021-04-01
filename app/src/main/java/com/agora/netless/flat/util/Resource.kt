package com.agora.netless.flat.util

data class Resource<out T>(
    val status: Status,
    val data: T?,
    val error: Throwable?,
    val message: String?
) {

    enum class Status {
        SUCCESS,
        ERROR,
        LOADING
    }

    companion object {
        fun <T> success(data: T?): Resource<T> {
            return Resource(Status.SUCCESS, data, null, null)
        }

        fun <T> error(data: T? = null, message: String, error: Throwable?): Resource<T> {
            return Resource(Status.ERROR, data, error, message)
        }

        fun <T> loading(data: T? = null): Resource<T> {
            return Resource(Status.LOADING, data, null, null)
        }
    }

    override fun toString(): String {
        return "Resource(status=$status, data=$data, error=$error, message=$message)"
    }
}