package link.netless.flat.http

interface HeaderProvider {
    fun getHeaders(): Set<Pair<String, String>>;
}