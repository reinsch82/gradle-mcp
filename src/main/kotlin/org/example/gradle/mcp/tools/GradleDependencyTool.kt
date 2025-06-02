package org.example.gradle.mcp.tools

import org.example.gradle.mcp.protocol.GradleTool
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import org.apache.commons.exec.*
import java.io.ByteArrayOutputStream
import java.io.File

@Component
class GradleDependencyTool : GradleTool {

    private val objectMapper = ObjectMapper()

    override fun getDescription(): String {
        return "Analyze and manage Gradle project dependencies. Can list dependencies, check for updates, and analyze dependency conflicts."
    }

    override fun getInputSchema(): Map<String, Any> {
        return mapOf(
            "type" to "object",
            "properties" to mapOf(
                "projectPath" to mapOf(
                    "type" to "string",
                    "description" to "Path to the Gradle project directory (defaults to current context)"
                ),
                "action" to mapOf(
                    "type" to "string",
                    "description" to "Action to perform: list, tree, insight, outdated, vulnerabilities",
                    "default" to "list"
                ),
                "configuration" to mapOf(
                    "type" to "string",
                    "description" to "Dependency configuration to analyze"
                ),
                "dependency" to mapOf(
                    "type" to "string",
                    "description" to "Specific dependency for insight analysis"
                )
            )
        )
    }

    override fun execute(arguments: JsonNode?): String {
        val projectPath = arguments?.get("projectPath")?.asText() 
            ?: GradleProjectContextTool.getCurrentProjectContext()
        val action = arguments?.get("action")?.asText() ?: "list"
        val configuration = arguments?.get("configuration")?.asText() ?: ""
        
        return try {
            when (action) {
                "list" -> listDependencies(projectPath, configuration)
                "tree" -> dependencyTree(projectPath, configuration)
                "insight" -> dependencyInsight(projectPath, arguments?.get("dependency")?.asText() ?: "")
                "outdated" -> outdatedDependencies(projectPath)
                "vulnerabilities" -> checkVulnerabilities(projectPath)
                else -> "Unknown action: $action. Supported actions: list, tree, insight, outdated, vulnerabilities"
            }
        } catch (e: Exception) {
            "Error analyzing dependencies: ${e.message}"
        }
    }

    private fun listDependencies(projectPath: String, configuration: String): String {
        val task = if (configuration.isNotEmpty()) {
            "dependencies --configuration $configuration"
        } else {
            "dependencies"
        }
        
        return executeGradleCommand(projectPath, task, "List Dependencies")
    }

    private fun dependencyTree(projectPath: String, configuration: String): String {
        val task = if (configuration.isNotEmpty()) {
            "dependencyInsight --configuration $configuration"
        } else {
            "dependencies --configuration runtimeClasspath"
        }
        
        return executeGradleCommand(projectPath, task, "Dependency Tree")
    }

    private fun dependencyInsight(projectPath: String, dependency: String): String {
        if (dependency.isEmpty()) {
            return objectMapper.writeValueAsString(mapOf(
                "success" to false,
                "error" to "Dependency name is required for insight analysis"
            ))
        }
        
        val task = "dependencyInsight --dependency $dependency"
        return executeGradleCommand(projectPath, task, "Dependency Insight")
    }

    private fun outdatedDependencies(projectPath: String): String {
        val task = "dependencyUpdates"
        return executeGradleCommand(projectPath, task, "Outdated Dependencies", allowFailure = true)
    }

    private fun checkVulnerabilities(projectPath: String): String {
        val task = "dependencyCheckAnalyze"
        return executeGradleCommand(projectPath, task, "Vulnerability Check", allowFailure = true)
    }

    private fun executeGradleCommand(
        projectPath: String, 
        task: String, 
        description: String,
        allowFailure: Boolean = false
    ): String {
        val projectDir = File(projectPath)
        
        if (!projectDir.exists()) {
            return objectMapper.writeValueAsString(mapOf(
                "success" to false,
                "error" to "Project directory does not exist: $projectPath"
            ))
        }

        val gradleCommand = when {
            File(projectDir, "gradlew").exists() -> "./gradlew"
            File(projectDir, "gradlew.bat").exists() -> "gradlew.bat"
            else -> "gradle"
        }

        val commandLine = CommandLine.parse("$gradleCommand $task")
        val executor = DefaultExecutor()
        executor.workingDirectory = projectDir
        
        val watchdog = ExecuteWatchdog(300000L)
        executor.watchdog = watchdog
        
        val outputStream = ByteArrayOutputStream()
        val errorStream = ByteArrayOutputStream()
        val pumpStreamHandler = PumpStreamHandler(outputStream, errorStream)
        executor.streamHandler = pumpStreamHandler

        return try {
            val startTime = System.currentTimeMillis()
            val exitCode = executor.execute(commandLine)
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime

            val result = mapOf(
                "success" to (exitCode == 0),
                "description" to description,
                "exitCode" to exitCode,
                "duration" to duration,
                "command" to "$gradleCommand $task",
                "workingDirectory" to projectDir.absolutePath,
                "output" to outputStream.toString("UTF-8"),
                "error" to errorStream.toString("UTF-8")
            )

            objectMapper.writeValueAsString(result)
        } catch (e: ExecuteException) {
            val result = mapOf(
                "success" to allowFailure,
                "description" to description,
                "exitCode" to e.exitValue,
                "command" to "$gradleCommand $task",
                "workingDirectory" to projectDir.absolutePath,
                "output" to outputStream.toString("UTF-8"),
                "error" to errorStream.toString("UTF-8") + "\nException: ${e.message}",
                "note" to if (!allowFailure) "Command failed" else "Command failed but this may be expected if the required plugin is not installed"
            )
            objectMapper.writeValueAsString(result)
        } catch (e: Exception) {
            val result = mapOf(
                "success" to false,
                "description" to description,
                "command" to "$gradleCommand $task",
                "workingDirectory" to projectDir.absolutePath,
                "output" to outputStream.toString("UTF-8"),
                "error" to "Execution failed: ${e.message}"
            )
            objectMapper.writeValueAsString(result)
        }
    }
}
