package Framework

import Model.JsonString
import Model.JsonValue.Companion.inferToJson
import java.net.ServerSocket
import java.net.Socket
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions

class GetJson(private vararg val controllers: Any) {
    private val endpoints = mutableListOf<Endpoint>()

    init {
        controllers.forEach { registerController(it) }
    }

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

    fun start(port: Int) {
        ServerSocket(port).use { server ->
            while (true) {
                server.accept().use { client ->
                    handleRequest(client)
                }
            }
        }
    }

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

    private fun invokeEndpoint(endpoint: Endpoint, queryParams: Map<String, String>): Any? {
        val requestPath = endpoint.path // Adicione esta linha para obter o caminho completo
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

    private fun extractPathParam(endpointPath: String, paramName: String): String? {
        val pattern = "\\($paramName\\)".toRegex()
        val match = pattern.find(endpointPath)
        return match?.value?.removeSurrounding("(", ")")
    }

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

    private fun extractPathParam(endpointPath: String, paramName: String, requestPath: String): String? {
        val endpointParts = endpointPath.split("/")
        val requestParts = requestPath.split("/")

        endpointParts.forEachIndexed { index, part ->
            if (part == "($paramName)") {
                return requestParts.getOrNull(index)
            }
        }
        return null
    }
}

data class Endpoint(
    val path: String,
    val method: KFunction<*>,
    val instance: Any
)