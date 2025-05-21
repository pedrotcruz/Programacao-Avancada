package Model

import kotlin.reflect.full.memberProperties

/**
 * Sealed class representing all valid JSON value types.
 *
 * This is the core class of the JSON library, following the Visitor pattern
 * for structure traversal and operations.
 */
sealed class JsonValue {
    /**
     * Accepts a visitor for processing this JSON value.
     * @param visitor The visitor implementation to handle this value
     */
    abstract fun accept(visitor: JsonVisitor)

    /**
     * Converts this JSON value to its standard JSON string representation.
     * @return The JSON-formatted string
     */
    abstract fun toJsonString(): String

    companion object {
        /**
         * Creates a JsonValue from any Kotlin object using reflection.
         * @param value The nullable input value to convert
         * @return The corresponding JsonValue representation
         * @throws IllegalArgumentException for unsupported types
         */
        fun from(value: Any?): JsonValue = value.inferToJson()

        /**
         * Extension function to convert Kotlin objects to JsonValue.
         * Supports:
         * - Primitives (Int, Double, Boolean, String)
         * - Collections
         * - Enums
         * - Null values
         * - Data classes
         * - Maps with String keys
         */
        fun Any?.inferToJson(): JsonValue {
            return when (this) {
                null -> JsonNull
                is Boolean -> JsonBoolean(this)
                is Number -> JsonNumber(this)
                is String -> JsonString(this)
                is Enum<*> -> JsonString(this.name)
                is Map<*, *> -> handleMap(this)
                is Iterable<*> -> handleIterable(this)
                is JsonValue -> this
                else -> handleDataClass(this)
            }
        }

        private fun handleMap(map: Map<*, *>): JsonObject {
            return JsonObject(map.mapKeys { it.key.toString() }
                .mapValues { it.value.inferToJson() })
        }

        private fun handleIterable(iterable: Iterable<*>): JsonArray {
            return JsonArray(iterable.map { it.inferToJson() })
        }

        private fun handleDataClass(obj: Any): JsonObject {
            if (!obj::class.isData) {
                throw IllegalArgumentException(
                    "Only data classes can be converted to JSON. Received: ${obj::class.simpleName}"
                )
            }
            val properties = obj::class.memberProperties
                .associate { prop ->
                    prop.name to prop.call(obj).inferToJson()
                }
            return JsonObject(properties)
        }
    }
}

/**
 * Represents a JSON object containing key-value pairs.
 * @property properties The map of property names to JSON values
 */
data class JsonObject(
    val properties: Map<String, JsonValue>
) : JsonValue() {

    override fun accept(visitor: JsonVisitor) = visitor.visit(this)

    override fun toJsonString(): String {
        return properties.entries.joinToString(",", "{", "}") {
            "\"${it.key}\":${it.value.toJsonString()}"
        }
    }

    /**
     * Filters the object's properties based on a predicate.
     * @param predicate Function to determine if a property should be included
     * @return A new JsonObject with only the matching properties
     */
    fun filter(predicate: (String, JsonValue) -> Boolean): JsonObject {
        val result = mutableMapOf<String, JsonValue>()
        properties.forEach { (key, value) ->
            if (predicate(key, value)) {
                result[key] = value
            }
        }
        return JsonObject(result)
    }
}

/**
 * Represents a JSON array of values.
 * @property elements The list of JSON values in the array
 */
data class JsonArray(val elements: List<JsonValue>) : JsonValue() {

    override fun accept(visitor: JsonVisitor) {
        visitor.visit(this)
    }

    override fun toJsonString(): String {
        return elements.joinToString(",", "[", "]") { it.toJsonString() }
    }

    /**
     * Filters array elements based on a predicate.
     * @param predicate Function to determine if an element should be included
     * @return A new JsonArray with only the matching elements
     */
    fun filter(predicate: (JsonValue) -> Boolean): JsonArray {
        val result = mutableListOf<JsonValue>()
        elements.forEach { element ->
            if (predicate(element)) {
                result.add(element)
            }
        }
        return JsonArray(result)
    }

    /**
     * Transforms array elements using a mapping function.
     * @param transform Function to apply to each element
     * @return A new JsonArray with transformed elements
     */
    fun map(transform: (JsonValue) -> JsonValue): JsonArray {
        val result = mutableListOf<JsonValue>()
        elements.forEach { element ->
            result.add(transform(element))
        }
        return JsonArray(result)
    }
}

/**
 * Represents a JSON string value.
 * @property value The string content
 */
data class JsonString(val value: String) : JsonValue() {
    override fun accept(visitor: JsonVisitor) = visitor.visit(this)

    override fun toJsonString(): String {
        return "\"${value.replace("\"", "\\\"")}\""
    }
}

/**
 * Represents a JSON number value.
 * @property value The numeric value
 */
data class JsonNumber(val value: Number) : JsonValue() {
    override fun accept(visitor: JsonVisitor) = visitor.visit(this)

    override fun toJsonString(): String = value.toString()
}

/**
 * Represents a JSON boolean value.
 * @property value The boolean value
 */
data class JsonBoolean(val value: Boolean) : JsonValue() {
    override fun accept(visitor: JsonVisitor) = visitor.visit(this)

    override fun toJsonString(): String = value.toString()
}

/**
 * Represents a JSON null value.
 */
object JsonNull : JsonValue() {
    override fun accept(visitor: JsonVisitor) = visitor.visit(this)

    override fun toJsonString(): String = "null"
}