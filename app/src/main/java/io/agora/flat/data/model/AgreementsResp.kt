package io.agora.flat.data.model

data class AgreementsResp(
    val data: Map<String, Map<String, Agreement>>
)

data class Agreement(
    val title: String,
    val message: String,
)