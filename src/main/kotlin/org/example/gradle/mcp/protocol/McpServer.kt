package org.example.gradle.mcp.protocol

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter

/**
 * Basic MCP (Model Context Protocol) server implementation
 * Handles JSON-RPC communication over STDIO
 */
@Component
class McpServer(
    private val toolRegistry: ToolRegistry,
    private val resourceRegistry: ResourceRegistry,
    private val promptRegistry: PromptRegistry
) {

    private val objectMapper = ObjectMapper().registerKotlinModule()

    fun start() {
        val reader = BufferedReader(InputStreamReader(System.`in`))
        val writer = PrintWriter(System.out, true)

        reader.lineSequence().forEach { line ->
            try {
                val request = objectMapper.readTree(line)
                val response = handleRequest(request)
                writer.println(objectMapper.writeValueAsString(response))
            } catch (e: Exception) {
                val errorResponse = createErrorResponse(null, -32603, "Internal error: ${e.message}")
                writer.println(objectMapper.writeValueAsString(errorResponse))
            }
        }
    }

    private fun handleRequest(request: JsonNode): Map<String, Any?> {
        val method = request["method"]?.asText()
        val id = request["id"]
        val params = request["params"]

        return when (method) {
            "initialize" -> handleInitialize(id)
            "tools/list" -> handleToolsList(id)
            "tools/call" -> handleToolCall(id, params)
            "resources/list" -> handleResourcesList(id)
            "resources/read" -> handleResourceRead(id, params)
            "prompts/list" -> handlePromptsList(id)
            "prompts/get" -> handlePromptGet(id, params)
            else -> createErrorResponse(id, -32601, "Method not found: $method")
        }
    }

    private fun handleInitialize(id: JsonNode?): Map<String, Any?> {
        return mapOf(
            "jsonrpc" to "2.0",
            "id" to id,
            "result" to mapOf(
                "protocolVersion" to "2024-11-05",
                "capabilities" to mapOf(
                    "tools" to mapOf("listChanged" to true),
                    "resources" to mapOf("subscribe" to false, "listChanged" to true),
                    "prompts" to mapOf("listChanged" to true)
                ),
                "serverInfo" to mapOf(
                    "name" to "gradle-mcp",
                    "version" to "1.0.0"
                )
            )
        )
    }

    private fun handleToolsList(id: JsonNode?): Map<String, Any?> {
        return mapOf(
            "jsonrpc" to "2.0",
            "id" to id,
            "result" to mapOf(
                "tools" to toolRegistry.listTools()
            )
        )
    }

    private fun handleToolCall(id: JsonNode?, params: JsonNode?): Map<String, Any?> {
        val toolName = params?.get("name")?.asText()
        val arguments = params?.get("arguments")

        if (toolName == null) {
            return createErrorResponse(id, -32602, "Missing tool name")
        }

        return try {
            val result = toolRegistry.executeTool(toolName, arguments)
            mapOf(
                "jsonrpc" to "2.0",
                "id" to id,
                "result" to mapOf(
                    "content" to listOf(
                        mapOf(
                            "type" to "text",
                            "text" to result
                        )
                    )
                )
            )
        } catch (e: Exception) {
            createErrorResponse(id, -32603, "Tool execution failed: ${e.message}")
        }
    }

    private fun handleResourcesList(id: JsonNode?): Map<String, Any?> {
        return mapOf(
            "jsonrpc" to "2.0",
            "id" to id,
            "result" to mapOf(
                "resources" to resourceRegistry.listResources()
            )
        )
    }

    private fun handleResourceRead(id: JsonNode?, params: JsonNode?): Map<String, Any?> {
        val uri = params?.get("uri")?.asText()

        if (uri == null) {
            return createErrorResponse(id, -32602, "Missing resource URI")
        }

        return try {
            val content = resourceRegistry.readResource(uri)
            mapOf(
                "jsonrpc" to "2.0",
                "id" to id,
                "result" to mapOf(
                    "contents" to listOf(
                        mapOf(
                            "uri" to uri,
                            "mimeType" to "text/plain",
                            "text" to content
                        )
                    )
                )
            )
        } catch (e: Exception) {
            createErrorResponse(id, -32603, "Resource read failed: ${e.message}")
        }
    }

    private fun handlePromptsList(id: JsonNode?): Map<String, Any?> {
        return mapOf(
            "jsonrpc" to "2.0",
            "id" to id,
            "result" to mapOf(
                "prompts" to promptRegistry.listPrompts()
            )
        )
    }

    private fun handlePromptGet(id: JsonNode?, params: JsonNode?): Map<String, Any?> {
        val name = params?.get("name")?.asText()
        val arguments = params?.get("arguments")

        if (name == null) {
            return createErrorResponse(id, -32602, "Missing prompt name")
        }

        return try {
            val prompt = promptRegistry.getPrompt(name, arguments)
            mapOf(
                "jsonrpc" to "2.0",
                "id" to id,
                "result" to mapOf(
                    "description" to "Generated prompt for $name",
                    "messages" to listOf(
                        mapOf(
                            "role" to "user",
                            "content" to mapOf(
                                "type" to "text",
                                "text" to prompt
                            )
                        )
                    )
                )
            )
        } catch (e: Exception) {
            createErrorResponse(id, -32603, "Prompt generation failed: ${e.message}")
        }
    }

    private fun createErrorResponse(id: JsonNode?, code: Int, message: String): Map<String, Any?> {
        return mapOf(
            "jsonrpc" to "2.0",
            "id" to id,
            "error" to mapOf(
                "code" to code,
                "message" to message
            )
        )
    }
}
