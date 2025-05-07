package Visitors

import Model.*

/**
 * Visitor that validates JSON structure, checking for:
 * - Empty keys
 * - Duplicate keys in objects
 */
class JsonValidator : JsonVisitor {
    private val errors = mutableListOf<String>()
    private val keyStack = mutableListOf<MutableSet<String>>()

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
        val currentKeys = mutableSetOf<String>()
        keyStack.add(currentKeys)

        obj.properties.forEach { (key, value) ->
            when {
                key.isEmpty() -> errors.add("Empty key found")
                currentKeys.contains(key) -> errors.add("Duplicate key found: '$key'")
                else -> {
                    currentKeys.add(key)
                    value.accept(this)
                }
            }
        }

        keyStack.removeAt(keyStack.size - 1)
    }

    override fun visit(arr: JsonArray) = arr.elements.forEach { it.accept(this) }
    override fun visit(str: JsonString) = Unit
    override fun visit(num: JsonNumber) = Unit
    override fun visit(bool: JsonBoolean) = Unit
    override fun visit(nullValue: JsonNull) = Unit
}