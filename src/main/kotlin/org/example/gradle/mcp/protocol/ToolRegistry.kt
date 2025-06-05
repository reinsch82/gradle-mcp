package org.example.gradle.mcp.protocol

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.example.gradle.mcp.tools.*
import org.example.gradle.mcp.config.GradleShellConfig
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired

@Component
class ToolRegistry @Autowired constructor(
    private val shellConfig: GradleShellConfig
) {

    private val objectMapper = ObjectMapper()
    private val tools = mutableMapOf<String, GradleTool>()

    init {
        // Register all Gradle tools
        registerTool("gradle_project_info", GradleProjectTool())
        registerTool("gradle_build", GradleBuildTool())
        registerTool("gradle_dependencies", GradleDependencyTool())
        registerTool("gradle_tasks", GradleTaskTool())
        registerTool("gradle_analysis", GradleAnalysisTool())
        registerTool("gradle_project_context", GradleProjectContextTool())
        registerTool("gradle_shell", GradleShellTool(shellConfig))
    }

    private fun registerTool(name: String, tool: GradleTool) {
        tools[name] = tool
    }

    fun listTools(): List<Map<String, Any>> {
        return tools.map { (name, tool) ->
            mapOf(
                "name" to name,
                "description" to tool.getDescription(),
                "inputSchema" to tool.getInputSchema()
            )
        }
    }

    fun executeTool(name: String, arguments: JsonNode?): String {
        val tool = tools[name] ?: throw IllegalArgumentException("Tool not found: $name")
        return tool.execute(arguments)
    }
}

interface GradleTool {
    fun getDescription(): String
    fun getInputSchema(): Map<String, Any>
    fun execute(arguments: JsonNode?): String
}
