package Visitors

import Model.*

/**
 * Visitor that analyzes array elements to determine if they have uniform types.
 *
 * Usage:
 * val checker = ArrayTypeChecker()
 * json.accept(checker)
 * val types = checker.getArrayTypes() // Map<JsonArray, Class<*>>
 */
class ArrayTypeChecker : JsonVisitor {
    private val arrayTypes = mutableMapOf<JsonArray, Class<*>>()

    /**
     * Gets the detected array element types.
     * @return Map where keys are arrays and values are the detected element type
     *         or Any::class.java for mixed/empty arrays
     */
    fun getArrayTypes(): Map<JsonArray, Class<*>> = arrayTypes.toMap()

    override fun visit(obj: JsonObject) = obj.properties.values.forEach { it.accept(this) }

    override fun visit(arr: JsonArray) {
        arrayTypes[arr] = when {
            arr.elements.isEmpty() -> Any::class.java
            arr.elements.all { it.javaClass == arr.elements.first().javaClass && it !is JsonNull } ->
                arr.elements.first().javaClass

            else -> Any::class.java
        }
        arr.elements.forEach { it.accept(this) }
    }

    override fun visit(str: JsonString) = Unit
    override fun visit(num: JsonNumber) = Unit
    override fun visit(bool: JsonBoolean) = Unit
    override fun visit(nullValue: JsonNull) = Unit
}