package Model

/**
 * Visitor interface for processing different JSON value types.
 * Follows the Visitor pattern for operations on JSON structures.
 *
 * Implement this interface to create operations that traverse and process JSON data.
 * Each method corresponds to a specific JSON type.
 */
interface JsonVisitor {
    /**
     * Processes a JSON object (key-value pairs).
     * @param obj The JSON object being visited
     */
    fun visit(obj: JsonObject)

    /**
     * Processes a JSON array (ordered list of values).
     * @param arr The JSON array being visited
     */
    fun visit(arr: JsonArray)

    /**
     * Processes a JSON string value.
     * @param str The string value being visited
     */
    fun visit(str: JsonString)

    /**
     * Processes a JSON number value.
     * @param num The number value being visited
     */
    fun visit(num: JsonNumber)

    /**
     * Processes a JSON boolean value.
     * @param bool The boolean value being visited
     */
    fun visit(bool: JsonBoolean)

    /**
     * Processes a JSON null value.
     * @param nullValue The null value being visited
     */
    fun visit(nullValue: JsonNull)
}