package com.magazineforge.app.models

data class VerifyKeyRequest(
    val gemini_api_key: String
)

data class VerifyKeyResponse(
    val valid: Boolean,
    val models: List<String>? = null
)
