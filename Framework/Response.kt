package Framework

import Model.JsonValue

/**
 * Wrapper for HTTP responses
 */
data class HttpResponse(
    val status: Int,
    val body: JsonValue,
    val headers: Map<String, String> = mapOf(
        "Content-Type" to "application/json"
    )
)