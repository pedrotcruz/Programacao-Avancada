import Framework.GetJson
import Framework.GetMapping
import Framework.RestController
import Model.JsonValue.Companion.inferToJson
import Visitors.*
import java.net.BindException

/**
 * Example enum representing evaluation types in a course.
 */
enum class EvalType {
    TEST,      // Written test evaluation
    PROJECT,   // Practical project evaluation
    EXAM       // Final exam evaluation
}

/**
 * Represents an evaluation item in a course.
 * @property name The name of the evaluation
 * @property percentage Weight in the final grade (0.0-1.0)
 * @property mandatory Whether the evaluation is mandatory
 * @property type The evaluation type (nullable)
 */
data class EvalItem(
    val name: String,
    val percentage: Double,
    val mandatory: Boolean,
    val type: EvalType?
)

/**
 * Represents a university course.
 * @property name Course name
 * @property credits ECTS credits
 * @property evaluation List of evaluation items
 */
data class Course(
    val name: String,
    val credits: Int,
    val evaluation: List<EvalItem>
)

/**
 * Main function demonstrating JSON serialization and validation.
 */
fun main() {
    val course = Course(
        name = "Programação Avançada",
        credits = 6,
        evaluation = listOf(
            EvalItem(
                name = "Teste Teórico",
                percentage = 0.4,
                mandatory = true,
                type = EvalType.TEST
            ),
            EvalItem(
                name = "Trabalho Prático",
                percentage = 0.6,
                mandatory = true,
                type = EvalType.PROJECT
            )
        )
    )

    // Convert to JSON model
    val json = course.inferToJson()

    // 1. Show compact JSON
    println("=== JSON Serializado ===")
    println(json.toJsonString())

    // 2. Show formatted JSON
    println("\n=== JSON Formatado ===")
    json.accept(JsonPrintVisitor())

    // 3. Validate structure
    println("\n=== Validação ===")
    val validator = JsonValidator()
    json.accept(validator)
    println("Válido? ${validator.isValid()}")
    validator.getErrors().forEach { println("Erro: $it") }


    // Inicia o servidor
    println("🟢 Starting server at http://localhost:8080")
    try {
        GetJson(
            CourseController(),
            AnotherController()
        ).start(8080)
    } catch (e: BindException) {
        println("🔴 Port 8080 already in use. Kill the process or wait.")
    } catch (e: Exception) {
        println("🔴 Failed to start: ${e.javaClass.simpleName}: ${e.message}")
    }
}

// Controller de exemplo
@RestController("courses")
class CourseController {
    @GetMapping("all")
    fun getAllCourses(): List<Course> = listOf(
        Course("Programação Avançada", 6, listOf(
            EvalItem("Teste", 0.4, true, EvalType.TEST),
            EvalItem("Projeto", 0.6, true, EvalType.PROJECT)
        )),
        Course("Sistemas Operativos", 6, listOf(
            EvalItem("Exame", 1.0, true, EvalType.EXAM)
        ))
    )

    @GetMapping("info")
    fun getCourseInfo() = mapOf(
        "version" to "1.0",
        "author" to "Seu Nome"
    )
}

@RestController("api")
class AnotherController {
    @GetMapping("status")
    fun status() = "Server is running"

    @GetMapping("/")
    fun root() = mapOf(
        "service" to "GetJson API",
        "endpoints" to listOf("/status", "/courses/all")
    )
}
