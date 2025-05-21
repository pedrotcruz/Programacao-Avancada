package Framework

import Model.JsonValue

/**
 * Represents an HTTP response
 * @property status HTTP status code
 * @property body JSON response body
 * @property headers Map of HTTP headers (defaults to JSON Content-Type)
 */
data class HttpResponse(
    val status: Int,
    val body: JsonValue,
    val headers: Map<String, String> = mapOf(
        "Content-Type" to "application/json"
    )
)