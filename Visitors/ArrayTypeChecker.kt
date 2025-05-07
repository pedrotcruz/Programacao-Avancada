package Visitors

import Model.*
import java.util.*

/**
 * Visitor that analyzes array elements to determine their uniform type.
 * Useful for schema validation or code generation.
 */
class ArrayTypeChecker : JsonVisitor {
    private val arrayTypes = mutableMapOf<JsonArray, Class<*>>()
    private var currentArray: JsonArray? = null

    /**
     * Gets the detected array types.
     * @return Map of arrays to their detected element types
     */
    fun getArrayTypes(): Map<JsonArray, Class<*>> = arrayTypes.toMap()

    override fun visit(obj: JsonObject) {
        obj.properties.values.forEach { it.accept(this) }
    }

    override fun visit(arr: JsonArray) {
        if (arr.elements.isEmpty()) {
            arrayTypes[arr] = Any::class.java
            return
        }

        val prevArray = currentArray
        currentArray = arr

        val firstType = arr.elements.first().javaClass
        val hasUniformType = arr.elements.all { it.javaClass == firstType && it !is JsonNull }

        arrayTypes[arr] = if (hasUniformType) firstType else Any::class.java

        arr.elements.forEach { it.accept(this) }
        currentArray = prevArray
    }

    override fun visit(str: JsonString) {}
    override fun visit(num: JsonNumber) {}
    override fun visit(bool: JsonBoolean) {}
    override fun visit(nullValue: JsonNull) {}
}