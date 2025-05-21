package Framework

import Model.JsonString
import Model.JsonValue.Companion.inferToJson
import java.net.ServerSocket
import java.net.Socket
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions

/**
 * Main framework class for handling HTTP GET endpoints that return JSON.
 *
 * @property controllers Vararg of REST controller instances to register
 */

class GetJson(private vararg val controllers: Any) {
    private val endpoints = mutableListOf<Endpoint>()

    init {
        controllers.forEach { registerController(it) }
    }

    /**
     * Registers all endpoints from a controller class
     * @param controller The controller instance to register
     * @RestController: Defines the path prefix for all routes in a controller.
     * @GetMapping: Specifies the relative path of each endpoint.
     */
    private fun registerController(controller: Any) {
        val controllerClass = controller::class
        val basePath = controllerClass.findAnnotation<RestController>()?.path ?: ""

        controllerClass.memberFunctions.forEach { function ->
            function.findAnnotation<GetMapping>()?.let { mapping ->
                val fullPath = when {
                    mapping.path == "/" -> basePath
                    else -> listOf(basePath, mapping.path.removePrefix("/"))
                        .joinToString("/")
                        .replace(Regex("/+"), "/")
                }
                endpoints.add(Endpoint(fullPath, function, controller))
            }
        }
    }

    /**
     * Starts the HTTP server on specified port
     * @param port The port to listen on
     */
    fun start(port: Int) {
        ServerSocket(port).use { server ->
            while (true) {
                server.accept().use { client ->
                    handleRequest(client)
                }
            }
        }
    }

    /**
     * Handles an incoming HTTP request
     * @param client The connected client socket
     */
    private fun handleRequest(client: Socket) {
        try {
            val input = client.getInputStream().bufferedReader()
            val request = input.readLine() ?: return

            val parts = request.split(" ")
            if (parts.size < 3) return

            val (method, path, _) = parts
            if (method != "GET") {
                sendResponse(client, HttpResponse(405, JsonString("Method Not Allowed")))
                return
            }

            val (cleanPath, queryParams) = parsePathAndQuery(path)

            findEndpoint(cleanPath)?.let { endpoint ->
                val result = invokeEndpoint(endpoint, queryParams)
                sendResponse(client, HttpResponse(200, result.inferToJson()))
            } ?: sendResponse(client, HttpResponse(404, JsonString("Not Found")))

        } catch (e: Exception) {
            sendResponse(client, HttpResponse(500, JsonString("Internal Server Error")))
        } finally {
            try {
                client.close()
            } catch (e: Exception) { /* Ignore close errors */ }
        }
    }

    /**
     * Parses a full path with query parameters into path and parameters map
     * @param fullPath The full request path including query parameters
     * @return Pair of (path without query, map of query parameters)
     */
    private fun parsePathAndQuery(fullPath: String): Pair<String, Map<String, String>> {
        val parts = fullPath.split("?")
        val path = parts[0]
        val query = parts.getOrElse(1) { "" }

        val queryParams = query.split("&")
            .associate {
                val pair = it.split("=")
                pair[0] to pair.getOrElse(1) { "" }
            }

        return path to queryParams
    }

    /**
     * Finds a matching endpoint for the given request path
     * @param path The request path to match
     * @return Endpoint if found, null otherwise
     */
    private fun findEndpoint(path: String): Endpoint? {
        return endpoints.find { endpoint ->
            val endpointParts = endpoint.path.split("/")
            val pathParts = path.removePrefix("/").split("/")

            endpointParts.size == pathParts.size &&
                    endpointParts.zip(pathParts).all { (ep, pp) ->
                        ep == pp || ep.startsWith("(") && ep.endsWith(")")
                    }
        }
    }

    /**
     * Invokes an endpoint method with extracted parameters
     * @param endpoint The endpoint to invoke
     * @param queryParams Map of query parameters
     * @return The result of the method call
     * @throws IllegalArgumentException if parameters are invalid
     */
    private fun invokeEndpoint(endpoint: Endpoint, queryParams: Map<String, String>): Any? {
        val requestPath = endpoint.path
        val args = endpoint.method.parameters.mapNotNull { param ->
            when {
                param.findAnnotation<Path>() != null -> {
                    val pathAnnotation = param.findAnnotation<Path>()!!
                    val pathVarName = pathAnnotation.name.takeIf { it.isNotEmpty() }
                        ?: throw IllegalArgumentException("Path parameter name must be specified")

                    // Extrai o valor do path usando a função auxiliar
                    extractPathParam(requestPath, pathVarName)
                        ?: throw IllegalArgumentException("Path parameter '$pathVarName' not found in URL")
                }

                param.findAnnotation<Param>() != null -> {
                    val paramAnnotation = param.findAnnotation<Param>()!!
                    val paramName = paramAnnotation.name.takeIf { it.isNotEmpty() }
                        ?: throw IllegalArgumentException("Query parameter name must be specified")

                    queryParams[paramName]?.let { convertParam(it, param.type) }
                        ?: throw IllegalArgumentException("Query parameter '$paramName' is required")
                }

                else -> null
            }
        }
        return endpoint.method.call(endpoint.instance, *args.toTypedArray())
    }

    /**
     * Extracts a path parameter value from the endpoint path
     * @param endpointPath The endpoint path template
     * @param paramName The parameter name to extract
     * @return The extracted parameter value or null if not found
     */
    private fun extractPathParam(endpointPath: String, paramName: String): String? {
        val pattern = "\\($paramName\\)".toRegex()
        val match = pattern.find(endpointPath)
        return match?.value?.removeSurrounding("(", ")")
    }

    /**
     * Converts a string parameter to the required type
     * @param value The string value to convert
     * @param type The target Kotlin type
     * @return The converted value
     * @throws IllegalArgumentException for unsupported or invalid types
     */
    private fun convertParam(value: String, type: KType): Any {
        return when (type.classifier) {
            Int::class -> value.toIntOrNull() ?: throw IllegalArgumentException("Invalid Int value")
            Long::class -> value.toLongOrNull() ?: throw IllegalArgumentException("Invalid Long value")
            Double::class -> value.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid Double value")
            Boolean::class -> value.toBooleanStrict()
            String::class -> value
            else -> throw IllegalArgumentException("Unsupported parameter type: $type")
        }
    }

    /**
     * Sends an HTTP response to the client
     * @param client The client socket to send response to
     * @param response The HTTP response to send
     */
    private fun sendResponse(client: Socket, response: HttpResponse) {
        val output = client.getOutputStream().bufferedWriter()
        output.apply {
            write("HTTP/1.1 ${response.status}\r\n")
            response.headers.forEach { (k, v) -> write("$k: $v\r\n") }
            write("\r\n")
            write(response.body.toJsonString())
            flush()
        }
    }
}

/**
 * Represents a registered endpoint
 * @property path The full endpoint path
 * @property method The reflected function to call
 * @property instance The controller instance containing the method
 */
data class Endpoint(
    val path: String,
    val method: KFunction<*>,
    val instance: Any
)