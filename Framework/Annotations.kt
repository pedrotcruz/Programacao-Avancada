package Framework

/**
 * Marks a class as a REST controller with base path
 * @property path The base path for all endpoints in this controller
 */
@Target(AnnotationTarget.CLASS)
annotation class RestController(val path: String = "")

/**
 * Marks a method as a GET endpoint
 * @property path The relative path for this endpoint
 */
@Target(AnnotationTarget.FUNCTION)
annotation class GetMapping(val path: String = "")

/**
 * Binds a method parameter to a path variable
 * @property name The name of the path variable (defaults to parameter name)
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Path(val name: String = "")

/**
 * Binds a method parameter to a query parameter
 * @property name The name of the query parameter (defaults to parameter name)
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Param(val name: String = "")