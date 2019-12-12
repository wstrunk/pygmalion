package net.rudoll.pygmalion.handlers.openapi.export

import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.examples.Example
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import net.rudoll.pygmalion.common.HttpCallMapper
import net.rudoll.pygmalion.model.StateHolder

object OpenApiMonitor {

    private val om = ObjectMapper()

    fun add(method: String, route: String, resultCallback: HttpCallMapper.ResultCallback) {
        val resultCallbackDescription = resultCallback.getResultCallbackDescription() ?: return
        val paths = StateHolder.state.openAPISpec.paths
        if (paths == null || paths[route] == null) {
            StateHolder.state.openAPISpec.path(route, PathItem())
        }
        addMethod(StateHolder.state.openAPISpec.paths[route]!!, method, resultCallbackDescription)
    }

    private fun addMethod(pathItem: PathItem, method: String, resultCallbackDescription: HttpCallMapper.ResultCallback.ResultCallbackDescription) {
        val operation = resultCallbackDescription.toOperation()
        when (method.toLowerCase()) {
            "get" -> pathItem.get(operation)
            "post" -> pathItem.post(operation)
            "put" -> pathItem.put(operation)
            "delete" -> pathItem.delete(operation)
            "options" -> pathItem.options(operation)
        }
    }

    internal fun getExample(rawExampleValue: String): Example {
        val example = Example()
        var exampleValue: Any = rawExampleValue
        try {
            exampleValue = om.readTree(rawExampleValue)
        } catch (e: Exception) {
            //ignore
        }
        example.value = exampleValue
        return example
    }

    fun getPrototype(): OpenAPI {
        val openApi = OpenAPI()
        openApi.info(getInfo())
        return openApi
    }

    private fun getInfo(): Info {
        val info = Info()
        info.version = "1.0"
        info.title = "Demo API"
        info.description = "Generated by Pygmalion"
        return info
    }

    fun addSecurityScheme(name: String, securityScheme: SecurityScheme) {
        ensureOpenApiSpecComponentsNotNull()
        StateHolder.state.openAPISpec.components.addSecuritySchemes(name, securityScheme)
    }

    fun addComponentSchemas(components: Components) {
        if (components.schemas == null) {
            return
        }
        components.schemas.forEach { addSchema(it.key, it.value) }
    }

    fun addSchema(key: String, schema: Schema<*>) {
        ensureOpenApiSpecComponentsNotNull()
        StateHolder.state.openAPISpec.components.addSchemas(key, schema)
    }

    private fun ensureOpenApiSpecComponentsNotNull() {
        if (StateHolder.state.openAPISpec.components == null) {
            StateHolder.state.openAPISpec.components = Components()
        }
    }

    fun setPort(port: Int) {
        val server = Server()
        server.url = "http://localhost:$port"
        StateHolder.state.openAPISpec.addServersItem(server)
    }
}

fun HttpCallMapper.ResultCallback.ResultCallbackDescription.toApiResponses(): ApiResponses {
    val mediaType = MediaType().addExamples("example1", OpenApiMonitor.getExample(this.exampleValue))
    val content = Content().addMediaType(this.contentType, mediaType)
    return ApiResponses().addApiResponse(statusCode.toString(), ApiResponse().description(description).content(content))
}

fun HttpCallMapper.ResultCallback.ResultCallbackDescription.toOperation(): Operation {
    if (this.operation != null) {
        return this.operation
    }
    val operation = Operation()
    operation.responses(this.toApiResponses())
    return operation
}