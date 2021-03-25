package com.agora.netless.flat.http

interface HeaderProvider {
    fun getHeaders(): Set<Pair<String, String>>;
}