package Testes

import Model.*
import Model.JsonValue.Companion.inferToJson
import Visitors.DebugVisitor
import Visitors.JsonPrintVisitor
import Visitors.JsonValidator
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows

/**
 * Unit tests for JSON inference functionality.
 */
class JsonInferenceTest {
    enum class TestEnum { CASE_A, CASE_B }

    data class Nested(val value: String)
    data class TestData(
        val a: String,
        val b: Int,
        val c: TestEnum,
        val d: Nested?,
        val e: List<Int>
    )

    /**
     * Tests conversion of a complex object with all supported field types.
     */
    @Test
    fun `test full object conversion`() {
        val obj = TestData(
            a = "text",
            b = 42,
            c = TestEnum.CASE_A,
            d = Nested("nested"),
            e = listOf(1, 2, 3)
        )

        val json = obj.inferToJson()

        assertTrue(json is JsonObject)
        val jsonObj = json as JsonObject

        assertEquals(JsonString("text"), jsonObj.properties["a"])
        assertEquals(JsonNumber(42), jsonObj.properties["b"])
        assertEquals(JsonString("CASE_A"), jsonObj.properties["c"])

        assertTrue(jsonObj.properties["d"] is JsonObject)
        val nested = jsonObj.properties["d"] as JsonObject
        assertEquals(JsonString("nested"), nested.properties["value"])

        assertTrue(jsonObj.properties["e"] is JsonArray)
        val list = jsonObj.properties["e"] as JsonArray
        assertEquals(3, list.elements.size)
        assertEquals(JsonNumber(1), list.elements[0])
    }

    @Test
    fun `test null conversion`() {
        val obj = TestData(
            a = "null_test",
            b = 0,
            c = TestEnum.CASE_A,
            d = null,
            e = emptyList()
        )

        val json = obj.inferToJson() as JsonObject
        assertEquals(JsonNull, json.properties["d"])
    }

    @Test
    fun `test primitive types`() {
        assertEquals(JsonString("test"), "test".inferToJson())
        assertEquals(JsonNumber(123), 123.inferToJson())
        assertEquals(JsonBoolean(true), true.inferToJson())
        assertEquals(JsonNull, null.inferToJson())
    }

    @Test
    fun `test map conversion`() {
        val map = mapOf(
            "key1" to "value1",
            "key2" to 42
        )
        val json = map.inferToJson() as JsonObject
        assertEquals(JsonString("value1"), json.properties["key1"])
        assertEquals(JsonNumber(42), json.properties["key2"])
    }

    @Test
    fun `test enum conversion`() {
        assertEquals(JsonString("CASE_A"), TestEnum.CASE_A.inferToJson())
    }

    @Test
    fun `test invalid type`() {
        val exception = assertThrows<IllegalArgumentException> {
            Any().inferToJson()
        }
        assertTrue(exception.message!!.contains("Only data classes can be converted"))
    }

    @Test
    fun `test visitor propagation`() {
        // Cria um JSON complexo com objetos, arrays e valores primitivos
        val json = mapOf(
            "name" to "Test",
            "values" to listOf(1, 2, 3),
            "nested" to mapOf(
                "flag" to true,
                "text" to "hello"
            )
        ).inferToJson()

        val debugVisitor = DebugVisitor()
        json.accept(debugVisitor) // Inicia a visita

        val expectedLog = listOf(
            "Visit Object (keys: [name, values, nested])",
            "Visit String: \"Test\"",
            "Visit Array (size: 3)",
            "Visit Number: 1",
            "Visit Number: 2",
            "Visit Number: 3",
            "Visit Object (keys: [flag, text])",
            "Visit Boolean: true",
            "Visit String: \"hello\""
        )

        assertEquals(expectedLog, debugVisitor.getLog())
    }

    @Test
    fun `test nested arrays`() {
        val json = listOf(
            listOf(1, "a"),
            listOf(true, null)
        ).inferToJson()

        val debugVisitor = DebugVisitor()
        json.accept(debugVisitor)

        val expectedLog = listOf(
            "Visit Array (size: 2)",
            "Visit Array (size: 2)",
            "Visit Number: 1",
            "Visit String: \"a\"",
            "Visit Array (size: 2)",
            "Visit Boolean: true",
            "Visit Null"
        )
        assertEquals(expectedLog, debugVisitor.getLog())
    }

    @Test
    fun `test objects in array`() {
        val json = listOf(
            mapOf("key" to 1),
            mapOf("key" to 2)
        ).inferToJson()

        val debugVisitor = DebugVisitor()
        json.accept(debugVisitor)

        val expectedLog = listOf(
            "Visit Array (size: 2)",
            "Visit Object (keys: [key])",
            "Visit Number: 1",
            "Visit Object (keys: [key])",
            "Visit Number: 2"
        )
        assertEquals(expectedLog, debugVisitor.getLog())
    }

    @Test
    fun `test empty structures`() {
        val json = mapOf(
            "emptyArray" to emptyList<Any>(),
            "emptyObject" to mapOf<String, Any>()
        ).inferToJson()

        val debugVisitor = DebugVisitor()
        json.accept(debugVisitor)

        val expectedLog = listOf(
            "Visit Object (keys: [emptyArray, emptyObject])",
            "Visit Array (size: 0)",
            "Visit Object (keys: [])"
        )
        assertEquals(expectedLog, debugVisitor.getLog())
    }

    @Test
    fun `test null values`() {
        val json = mapOf(
            "key" to null,
            "arrayWithNull" to listOf(1, null)
        ).inferToJson()

        val debugVisitor = DebugVisitor()
        json.accept(debugVisitor)

        val expectedLog = listOf(
            "Visit Object (keys: [key, arrayWithNull])",
            "Visit Null",
            "Visit Array (size: 2)",
            "Visit Number: 1",
            "Visit Null"
        )
        assertEquals(expectedLog, debugVisitor.getLog())
    }



    @Test
    fun `test print visitor output`() {
        val json = listOf(mapOf("key" to "value")).inferToJson()
        val printVisitor = JsonPrintVisitor()
        json.accept(printVisitor)

        // Capture a saída (usando `SystemOutRule` ou similar)
        // Verifique se contém quebras de linha e indentação esperadas.
    }

    @Test
    fun `test duplicate keys in different objects`() {
        val json = mapOf(
            "obj1" to mapOf("key" to 1),
            "obj2" to mapOf("key" to 2)  // Mesma chave, mas em objetos diferentes
        ).inferToJson()

        val validator = JsonValidator()
        json.accept(validator)

        assertTrue(validator.isValid())  // Não deve haver erros
    }

    @Test
    fun `test duplicate keys in different objects are allowed`() {
        val json = mapOf(
            "obj1" to mapOf("key" to 1),
            "obj2" to mapOf("key" to 2)  // Mesma chave, mas em objetos diferentes
        ).inferToJson()

        val validator = JsonValidator()
        json.accept(validator)

        assertTrue(validator.isValid())  // Não deve haver erros
    }

    @Test
    fun `test duplicate keys in different objects are ignored`() {
        val json = JsonObject(
            mapOf(
                "obj1" to JsonObject(mapOf("key" to JsonNumber(1))),
                "obj2" to JsonObject(mapOf("key" to JsonNumber(2)))
            )
        )

        val validator = JsonValidator()
        json.accept(validator)

        assertTrue(validator.isValid()) // Não deve reportar erro
    }
}
