package Testes

import Framework.GetJson
import Framework.GetMapping
import Framework.Path
import Framework.Param
import Framework.RestController
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.net.HttpURLConnection
import java.net.URL

class GetJsonTest {
    @RestController("test")
    class TestController {
        @GetMapping("hello")
        fun hello(): String = "Hello World"

        @GetMapping("user/(id)")
        fun getUser(@Path id: Int): Map<String, Any> = mapOf("id" to id, "name" to "User$id")

        @GetMapping("search")
        fun search(@Param q: String): List<String> = listOf(q, q.reversed())
    }

    @Test
    fun testFramework() {
        val server = GetJson(TestController())
        Thread { server.start(8080) }.start()
        Thread.sleep(500) // Wait for server to start

        // Test basic endpoint
        testEndpoint("/test/hello", "\"Hello World\"")

        // Test path parameter
        testEndpoint("/test/user/42", """{"id":42,"name":"User42"}""")

        // Test query parameter
        testEndpoint("/test/search?q=kotlin", """["kotlin","niltok"]""")

        // Test error cases
        testErrorEndpoint("/test/user/abc", 400) // Invalid path param
        testErrorEndpoint("/test/search", 400)    // Missing query param
        testErrorEndpoint("/test/unknown", 404)   // Not found
    }

    private fun testEndpoint(path: String, expected: String) {
        val connection = URL("http://localhost:8080$path").openConnection() as HttpURLConnection
        assertEquals(200, connection.responseCode)
        assertEquals(expected, connection.inputStream.bufferedReader().readText())
    }

    private fun testErrorEndpoint(path: String, expectedStatus: Int) {
        val connection = URL("http://localhost:8080$path").openConnection() as HttpURLConnection
        assertEquals(expectedStatus, connection.responseCode)
    }
}