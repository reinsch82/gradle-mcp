package org.example.gradle.mcp.tools

import org.example.gradle.mcp.protocol.GradleTool
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import org.apache.commons.exec.*
import java.io.ByteArrayOutputStream
import java.io.File

@Component
class GradleTaskTool : GradleTool {

    private val objectMapper = ObjectMapper()

    override fun getDescription(): String {
        return "List and analyze Gradle tasks available in the project. Can show all tasks, tasks by group, or detailed task information."
    }

    override fun getInputSchema(): Map<String, Any> {
        return mapOf(
            "type" to "object",
            "properties" to mapOf(
                "projectPath" to mapOf(
                    "type" to "string",
                    "description" to "Path to the Gradle project directory"
                ),
                "action" to mapOf(
                    "type" to "string",
                    "description" to "Action to perform: list, all, help, properties",
                    "default" to "list"
                ),
                "group" to mapOf(
                    "type" to "string",
                    "description" to "Task group to filter by (for list action)"
                ),
                "taskName" to mapOf(
                    "type" to "string",
                    "description" to "Specific task name (for help and properties actions)"
                )
            )
        )
    }

    override fun execute(arguments: JsonNode?): String {
        val projectPath = arguments?.get("projectPath")?.asText() ?: System.getProperty("user.dir")
        val action = arguments?.get("action")?.asText() ?: "list"
        val group = arguments?.get("group")?.asText() ?: ""
        val taskName = arguments?.get("taskName")?.asText() ?: ""
        
        return try {
            when (action) {
                "list" -> listTasks(projectPath, group)
                "all" -> listAllTasks(projectPath)
                "help" -> getTaskHelp(projectPath, taskName)
                "properties" -> getTaskProperties(projectPath, taskName)
                else -> "Unknown action: $action. Supported actions: list, all, help, properties"
            }
        } catch (e: Exception) {
            "Error analyzing tasks: ${e.message}"
        }
    }

    private fun listTasks(projectPath: String, group: String): String {
        val task = if (group.isNotEmpty()) {
            "tasks --group $group"
        } else {
            "tasks"
        }
        
        return executeGradleCommand(projectPath, task, "List Tasks")
    }

    private fun listAllTasks(projectPath: String): String {
        return executeGradleCommand(projectPath, "tasks --all", "List All Tasks")
    }

    private fun getTaskHelp(projectPath: String, taskName: String): String {
        if (taskName.isEmpty()) {
            return objectMapper.writeValueAsString(mapOf(
                "success" to false,
                "error" to "Task name is required for help"
            ))
        }
        
        return executeGradleCommand(projectPath, "help --task $taskName", "Task Help")
    }

    private fun getTaskProperties(projectPath: String, taskName: String): String {
        if (taskName.isEmpty()) {
            return objectMapper.writeValueAsString(mapOf(
                "success" to false,
                "error" to "Task name is required for properties"
            ))
        }
        
        return executeGradleCommand(projectPath, "$taskName --help", "Task Properties", allowFailure = true)
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
        
        val watchdog = ExecuteWatchdog(120000L)
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

            val output = outputStream.toString("UTF-8")
            val result = mapOf(
                "success" to (exitCode == 0),
                "description" to description,
                "exitCode" to exitCode,
                "duration" to duration,
                "command" to "$gradleCommand $task",
                "workingDirectory" to projectDir.absolutePath,
                "output" to output,
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
                "error" to errorStream.toString("UTF-8") + "\nException: ${e.message}"
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
