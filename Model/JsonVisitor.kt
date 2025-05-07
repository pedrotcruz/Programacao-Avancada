package Model

/**
 * Visitor interface for processing different JSON value types.
 * Follows the Visitor pattern for operations on JSON structures.
 */
interface JsonVisitor {
    /**
     * Visits a JSON object.
     * @param obj The JSON object to process
     */
    fun visit(obj: JsonObject)

    /**
     * Visits a JSON array.
     * @param arr The JSON array to process
     */
    fun visit(arr: JsonArray)

    /**
     * Visits a JSON string value.
     * @param str The string value to process
     */
    fun visit(str: JsonString)

    /**
     * Visits a JSON number value.
     * @param num The number value to process
     */
    fun visit(num: JsonNumber)

    /**
     * Visits a JSON boolean value.
     * @param bool The boolean value to process
     */
    fun visit(bool: JsonBoolean)

    /**
     * Visits a JSON null value.
     * @param nullValue The null value to process
     */
    fun visit(nullValue: JsonNull)
}