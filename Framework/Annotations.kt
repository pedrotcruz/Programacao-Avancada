package Framework

/**
 * Marks a class as a REST controller with base path
 */
@Target(AnnotationTarget.CLASS)
annotation class RestController(val path: String = "")

/**
 * Marks a method as a GET endpoint
 */
@Target(AnnotationTarget.FUNCTION)
annotation class GetMapping(val path: String = "")

/**
 * Binds a method parameter to a path variable
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Path(val name: String = "")

/**
 * Binds a method parameter to a query parameter
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Param(val name: String = "")