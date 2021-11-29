package io.agora.flat.http

/**
 * 通用请求头处理，支持多模块实现
 */
interface HeaderProvider {
    fun getHeaders(): Set<Pair<String, String>>
}