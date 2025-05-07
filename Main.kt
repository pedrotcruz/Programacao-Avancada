import Model.*
import Model.JsonValue.Companion.inferToJson
import Visitors.*

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
    json.accept(PrettyPrintVisitor())

    // 3. Validate structure
    println("\n=== Validação ===")
    val validator = JsonValidator()
    json.accept(validator)
    println("Válido? ${validator.isValid()}")
    validator.getErrors().forEach { println("Erro: $it") }
}