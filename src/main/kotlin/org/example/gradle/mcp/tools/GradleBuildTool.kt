package org.example.gradle.mcp.tools

import org.example.gradle.mcp.protocol.GradleTool
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import org.apache.commons.exec.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Duration

@Component
class GradleBuildTool : GradleTool {

    private val objectMapper = ObjectMapper()

    override fun getDescription(): String {
        return "Execute Gradle build commands and tasks. Supports common tasks like build, clean, test, assemble, etc."
    }

    override fun getInputSchema(): Map<String, Any> {
        return mapOf(
            "type" to "object",
            "properties" to mapOf(
                "projectPath" to mapOf(
                    "type" to "string",
                    "description" to "Path to the Gradle project directory"
                ),
                "task" to mapOf(
                    "type" to "string", 
                    "description" to "Gradle task to execute",
                    "default" to "build"
                ),
                "args" to mapOf(
                    "type" to "string",
                    "description" to "Additional arguments for the Gradle command"
                ),
                "timeout" to mapOf(
                    "type" to "number",
                    "description" to "Timeout in milliseconds",
                    "default" to 300000
                )
            )
        )
    }

    override fun execute(arguments: JsonNode?): String {
        val projectPath = arguments?.get("projectPath")?.asText() ?: System.getProperty("user.dir")
        val task = arguments?.get("task")?.asText() ?: "build"
        val additionalArgs = arguments?.get("args")?.asText() ?: ""
        val timeout = arguments?.get("timeout")?.asLong() ?: 300000L
        
        return try {
            executeGradleTask(projectPath, task, additionalArgs, timeout)
        } catch (e: Exception) {
            "Error executing Gradle task: ${e.message}"
        }
    }

    private fun executeGradleTask(projectPath: String, task: String, additionalArgs: String, timeout: Long): String {
        val projectDir = File(projectPath)
        
        if (!projectDir.exists()) {
            return objectMapper.writeValueAsString(mapOf(
                "success" to false,
                "error" to "Project directory does not exist: $projectPath"
            ))
        }

        // Determine gradle command
        val gradleCommand = when {
            File(projectDir, "gradlew").exists() -> "./gradlew"
            File(projectDir, "gradlew.bat").exists() -> "gradlew.bat"
            else -> "gradle"
        }

        val commandLine = CommandLine.parse("$gradleCommand $task $additionalArgs".trim())
        val executor = DefaultExecutor.builder().setWorkingDirectory(projectDir).get()
        

        // Set timeout
        val watchdog = ExecuteWatchdog.builder().setTimeout(Duration.ofSeconds(timeout)).get()
        executor.watchdog = watchdog
        
        // Capture output
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
                "exitCode" to exitCode,
                "duration" to duration,
                "command" to "$gradleCommand $task $additionalArgs".trim(),
                "workingDirectory" to projectDir.absolutePath,
                "output" to outputStream.toString("UTF-8"),
                "error" to errorStream.toString("UTF-8")
            )

            objectMapper.writeValueAsString(result)
        } catch (e: ExecuteException) {
            val result = mapOf(
                "success" to false,
                "exitCode" to e.exitValue,
                "command" to "$gradleCommand $task $additionalArgs".trim(),
                "workingDirectory" to projectDir.absolutePath,
                "output" to outputStream.toString("UTF-8"),
                "error" to errorStream.toString("UTF-8") + "\nException: ${e.message}"
            )
            objectMapper.writeValueAsString(result)
        } catch (e: Exception) {
            val result = mapOf(
                "success" to false,
                "command" to "$gradleCommand $task $additionalArgs".trim(),
                "workingDirectory" to projectDir.absolutePath,
                "output" to outputStream.toString("UTF-8"),
                "error" to "Execution failed: ${e.message}"
            )
            objectMapper.writeValueAsString(result)
        }
    }
}
