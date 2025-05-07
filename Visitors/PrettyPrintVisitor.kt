package Visitors

import Model.*

/**
 * Visitor that formats JSON values with proper indentation and line breaks.
 * Produces human-readable output for debugging or display purposes.
 */
class PrettyPrintVisitor : JsonVisitor {
    private var indentLevel = 0
    private val indent = "    "

    private fun currentIndent(): String = indent.repeat(indentLevel)

    override fun visit(obj: JsonObject) {
        println("{")
        indentLevel++
        val entries = obj.properties.entries.sortedBy { it.key }
        entries.forEachIndexed { index, (key, value) ->
            print("${currentIndent()}\"$key\": ")
            value.accept(this)
            if (index < entries.size - 1) println(",") else println()
        }
        indentLevel--
        print("${currentIndent()}}")
    }

    override fun visit(arr: JsonArray) {
        println("[")
        indentLevel++
        arr.elements.forEachIndexed { index, element ->
            print(currentIndent())
            element.accept(this)
            if (index < arr.elements.size - 1) println(",") else println()
        }
        indentLevel--
        print("${currentIndent()}]")
    }

    override fun visit(str: JsonString) = print(str.toJsonString())
    override fun visit(num: JsonNumber) = print(num.toJsonString())
    override fun visit(bool: JsonBoolean) = print(bool.toJsonString())
    override fun visit(nullValue: JsonNull) = print(nullValue.toJsonString())
}