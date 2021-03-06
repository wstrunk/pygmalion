package net.rudoll.pygmalion.common

import io.swagger.v3.oas.models.Operation
import net.rudoll.pygmalion.cli.Cli
import net.rudoll.pygmalion.handlers.arguments.parsedarguments.AllowCorsArgument
import net.rudoll.pygmalion.handlers.arguments.parsedarguments.ContentTypeArgument
import net.rudoll.pygmalion.handlers.arguments.parsedarguments.LogArgument
import net.rudoll.pygmalion.handlers.arguments.parsedarguments.LogBodyArgument
import net.rudoll.pygmalion.handlers.openapi.export.OpenApiMonitor
import net.rudoll.pygmalion.model.ParsedInput
import net.rudoll.pygmalion.model.StateHolder
import spark.Request
import spark.Response
import spark.Spark
import spark.Spark.halt

object HttpCallMapper {

    private val ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin"
    private val ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers"
    private val ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods"
    private val ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials"
    private val ORIGIN = "Origin"

    fun map(method: String, route: String, parsedInput: ParsedInput, resultCallback: ResultCallback) {
        PortManager.ensurePortIsSet(parsedInput)
        val requestHandler = { request: Request, response: Response -> handleCall(request, response, parsedInput, resultCallback) }
        parsedInput.logs.add("Mapping route $route for method $method")
        when (method.toLowerCase()) {
            "get" -> Spark.get(route, requestHandler)
            "post" -> Spark.post(route, requestHandler)
            "put" -> Spark.put(route, requestHandler)
            "delete" -> Spark.delete(route, requestHandler)
            "options" -> Spark.options(route, requestHandler)
            else -> {
                parsedInput.errors.add("Unknown method.")
                return
            }
        }
        OpenApiMonitor.add(method, route, resultCallback)
    }

    private fun handleCall(request: Request, response: Response, parsedInput: ParsedInput, resultCallback: ResultCallback): String {
        handleChaosMonkey()
        val arguments = parsedInput.arguments
        val shouldLog = arguments.contains(LogArgument) || arguments.contains(LogBodyArgument)
        val shouldAllowCORS = arguments.contains(AllowCorsArgument)
        if (shouldAllowCORS) {
            passAccessControl(request, response)
        }
        val result = resultCallback.getResult(request, response)
        if (shouldLog) {
            Cli.log("< ${DateFormatter.now()} | ${request.url()} | Status ${response.status()} | Body length: ${result.length}${if (arguments.contains(LogBodyArgument)) " | Body: ${request.body()} | Response: $result" else ""}")
        }
        setContentType(response, parsedInput)
        return result
    }

    private fun handleChaosMonkey() {
        if (Math.random() * 100 < StateHolder.state.chaosMonkeyProbability) {
            halt(503)
        }
    }

    private fun setContentType(response: Response, parsedInput: ParsedInput) {
        for (argument in parsedInput.arguments) {
            if (argument is ContentTypeArgument) {
                response.header("Content-Type", argument.contentType)
                break
            }
        }
    }

    interface ResultCallback {
        data class ResultCallbackDescription(val statusCode: Int, val description: String, val operation: Operation? = null, val contentType: String = "application/json", val exampleValue: String = "")

        fun getResult(request: Request, response: Response): String
        fun getResultCallbackDescription(): ResultCallbackDescription?
    }

    fun allowPreflightRequests(route: String) {
        Spark.options(route) { request, response -> successfulPreflight(request, response) }
    }

    private fun passAccessControl(request: Request, response: Response) {
        response.header(ACCESS_CONTROL_ALLOW_ORIGIN, request.headers(ORIGIN))
        response.header(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
    }

    private fun successfulPreflight(request: Request, response: Response): String {
        passAccessControl(request, response)
        response.header(ACCESS_CONTROL_ALLOW_HEADERS, "content-type, authorization")
        response.header(ACCESS_CONTROL_ALLOW_METHODS, "POST, GET, PUT, OPTIONS, DELETE")
        return ""
    }

    fun ensureAllQueryParamsPresent(request: Request, params: List<String>): Boolean {
        val queryParams = request.queryParams()
        for (param in params) {
            if (!queryParams.contains(param)) {
                halt(400, "Required parameter $param not present")
            }
        }
        return request.queryParams().containsAll(params)
    }
}
