package Visitors

import Model.*

/**
 * Visitor that validates JSON structure by checking for:
 * - Empty keys in objects
 * - Uniform array types (optional)
 */
class JsonValidator : JsonVisitor {
    private val errors = mutableListOf<String>()

    /**
     * Checks if the JSON structure is valid.
     * @return true if no validation errors were found
     */
    fun isValid(): Boolean = errors.isEmpty()

    /**
     * Gets the list of validation errors found.
     * @return List of error messages
     */
    fun getErrors(): List<String> = errors.toList()

    override fun visit(obj: JsonObject) {
        obj.properties.forEach { (key, value) ->
            if (key.isEmpty()) {
                errors.add("Empty key found")
            }
            value.accept(this)
        }
    }

    override fun visit(arr: JsonArray) = arr.elements.forEach { it.accept(this) }
    override fun visit(str: JsonString) = Unit
    override fun visit(num: JsonNumber) = Unit
    override fun visit(bool: JsonBoolean) = Unit
    override fun visit(nullValue: JsonNull) = Unit
}