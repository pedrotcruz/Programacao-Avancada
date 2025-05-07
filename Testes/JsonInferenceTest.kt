package Testes

import Model.*
import Model.JsonValue.Companion.inferToJson
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
}