package io.agora.flat.data.model

sealed class LoadState {
    object Loading : LoadState()
    data class NotLoading(val end: Boolean) : LoadState() {
        companion object {
            val Complete = NotLoading(end = true)
            val Incomplete = NotLoading(end = false)
        }
    }

    data class Error(val error: Throwable) : LoadState()
}