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

// User-supplied stock-photo keys awaiting verification in Settings. Both are
// optional and checked independently: a user with only a Pixabay account must
// be able to save that key while the Pexels box stays empty. An empty string
// means "not supplied" — the deployment's own key keeps being used.
data class VerifyImageKeysRequest(
    val pixabay_key: String = "",
    val pexels_key: String = ""
)

// Nullable on purpose: Gson builds these through Unsafe, so a Kotlin default
// never runs for a field the JSON omits and a declared non-null object would
// still arrive null. An unexpected response shape must read as "not verified",
// not crash Settings.
data class VerifyImageKeysResponse(
    val pixabay: ImageKeyResult? = null,
    val pexels: ImageKeyResult? = null
)

data class ImageKeyResult(
    val valid: Boolean = false,
    val status: String? = null
)
