package com.magazineforge.app.models

data class VerifyKeyRequest(
    val litellm_url: String,
    val litellm_key: String
)

data class VerifyKeyResponse(
    val valid: Boolean,
    val models: List<String>? = null,
    val status: String? = null
)
