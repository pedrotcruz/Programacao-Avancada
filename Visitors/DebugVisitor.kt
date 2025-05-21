package Visitors

import Model.*

/**
 * Visitor de debug que registra todas as chamadas de visita.
 * Útil para verificar a propagação em objetos JSON complexos.
 */
class DebugVisitor : JsonVisitor {
    private val log = mutableListOf<String>()

    fun getLog(): List<String> = log.toList()

    override fun visit(obj: JsonObject) {
        log.add("Visit Object (keys: ${obj.properties.keys})")
        obj.properties.values.forEach { it.accept(this) } // Propaga para os filhos
    }

    override fun visit(arr: JsonArray) {
        log.add("Visit Array (size: ${arr.elements.size})")
        arr.elements.forEach { it.accept(this) } // Propaga para os elementos
    }

    override fun visit(str: JsonString) {
        log.add("Visit String: \"${str.value}\"")
    }

    override fun visit(num: JsonNumber) {
        log.add("Visit Number: ${num.value}")
    }

    override fun visit(bool: JsonBoolean) {
        log.add("Visit Boolean: ${bool.value}")
    }

    override fun visit(nullValue: JsonNull) {
        log.add("Visit Null")
    }
}