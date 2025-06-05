package org.example.gradle.mcp.tools

import org.example.gradle.mcp.protocol.GradleTool
import org.example.gradle.mcp.config.GradleShellConfig
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import org.apache.commons.exec.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Duration
import java.util.regex.Pattern

@Component
class GradleShellTool @Autowired constructor(
    private val config: GradleShellConfig
) : GradleTool {

    private val objectMapper = ObjectMapper()
    
    // Safety patterns for command validation
    private val dangerousPatterns = listOf(
        Pattern.compile("rm\\s+-rf\\s+/", Pattern.CASE_INSENSITIVE),
        Pattern.compile("sudo", Pattern.CASE_INSENSITIVE),
        Pattern.compile("chmod\\s+777", Pattern.CASE_INSENSITIVE),
        Pattern.compile("mkfs", Pattern.CASE_INSENSITIVE),
        Pattern.compile("dd\\s+if=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("format\\s+", Pattern.CASE_INSENSITIVE)
    )

    override fun getDescription(): String {
        return "Execute shell commands in the Gradle project context with safety validation"
    }

    override fun getInputSchema(): Map<String, Any> {
        return mapOf(
            "type" to "object",
            "properties" to mapOf(
                "command" to mapOf(
                    "type" to "string",
                    "description" to "Shell command to execute"
                ),
                "projectPath" to mapOf(
                    "type" to "string",
                    "description" to "Path to the Gradle project directory (defaults to current context)"
                ),
                "timeout" to mapOf(
                    "type" to "number",
                    "description" to "Timeout in milliseconds",
                    "default" to 30000
                ),
                "workingDirectory" to mapOf(
                    "type" to "string",
                    "description" to "Working directory relative to project root (optional)"
                ),
                "environment" to mapOf(
                    "type" to "object",
                    "description" to "Additional environment variables",
                    "additionalProperties" to mapOf("type" to "string")
                )
            ),
            "required" to listOf("command")
        )
    }

    override fun execute(arguments: JsonNode?): String {
        // Check if shell tool is enabled
        if (!config.enabled) {
            return createErrorResponse(
                "Shell command execution is disabled",
                "disabled"
            )
        }

        val command = arguments?.get("command")?.asText()
            ?: return createErrorResponse("Command is required")
            
        val projectPath = arguments?.get("projectPath")?.asText()
            ?: GradleProjectContextTool.getCurrentProjectContext()
            
        val requestedTimeout = arguments?.get("timeout")?.asLong() ?: config.timeoutDefault
        val timeout = minOf(requestedTimeout, config.timeoutMax)
        
        val workingDirectory = arguments?.get("workingDirectory")?.asText()
        val environment = arguments?.get("environment")?.let { envNode ->
            envNode.fields().asSequence().associate { (key, value) ->
                key to value.asText()
            }
        } ?: emptyMap()

        return try {
            // Validate command for safety
            val validationResult = validateCommand(command)
            if (!validationResult.isValid) {
                return createErrorResponse(
                    "Command validation failed: ${validationResult.reason}",
                    "validation"
                )
            }

            executeShellCommand(command, projectPath, timeout, workingDirectory, environment)
        } catch (e: Exception) {
            createErrorResponse("Error executing shell command: ${e.message}")
        }
    }

    private fun validateCommand(command: String): ValidationResult {
        // Ensure command is not empty or just whitespace
        if (command.trim().isEmpty()) {
            return ValidationResult(false, "Command cannot be empty")
        }

        // Apply validation based on mode
        when (config.validationMode) {
            "strict" -> {
                // Check for dangerous patterns
                dangerousPatterns.forEach { pattern ->
                    if (pattern.matcher(command).find()) {
                        return ValidationResult(
                            false, 
                            "Command contains potentially dangerous pattern: ${pattern.pattern()}"
                        )
                    }
                }
            }
            "whitelist" -> {
                // Only allow commands in the whitelist
                val commandStart = command.trim().split("\\s+".toRegex()).first()
                if (!config.allowedCommands.any { allowed -> 
                    commandStart.startsWith(allowed) || command.startsWith(allowed) 
                }) {
                    return ValidationResult(
                        false, 
                        "Command not in whitelist. Allowed: ${config.allowedCommands.joinToString(", ")}"
                    )
                }
            }
            "permissive" -> {
                // Only basic safety checks
                if (command.contains("rm -rf /") || command.trim().startsWith("sudo")) {
                    return ValidationResult(false, "Extremely dangerous command blocked")
                }
            }
        }

        return ValidationResult(true, null)
    }

    private fun executeShellCommand(
        command: String,
        projectPath: String,
        timeout: Long,
        workingDirectory: String?,
        environment: Map<String, String>
    ): String {
        val projectDir = File(projectPath)
        
        if (!projectDir.exists()) {
            return createErrorResponse(
                "Project directory does not exist: $projectPath",
                "not_found"
            )
        }

        // Determine working directory
        val workDir = if (workingDirectory != null) {
            val targetDir = File(projectDir, workingDirectory)
            if (!targetDir.exists()) {
                return createErrorResponse(
                    "Working directory does not exist: ${targetDir.absolutePath}",
                    "not_found"
                )
            }
            // Security check: ensure working directory is within project (if restriction enabled)
            if (config.workingDirectoryRestriction && 
                !targetDir.canonicalPath.startsWith(projectDir.canonicalPath)) {
                return createErrorResponse(
                    "Working directory must be within project directory",
                    "permission"
                )
            }
            targetDir
        } else {
            projectDir
        }

        val commandLine = CommandLine.parse(command)
        val executor = DefaultExecutor.builder().setWorkingDirectory(workDir).get()

        // Set timeout
        val watchdog = ExecuteWatchdog.builder().setTimeout(Duration.ofMillis(timeout)).get()
        executor.watchdog = watchdog

        // Capture output
        val outputStream = ByteArrayOutputStream()
        val errorStream = ByteArrayOutputStream()
        val pumpStreamHandler = PumpStreamHandler(outputStream, errorStream)
        executor.streamHandler = pumpStreamHandler

        // Set up environment
        val env = mutableMapOf<String, String>()
        
        // Add configured passthrough environment variables
        config.environmentPassthrough.forEach { varName ->
            System.getenv(varName)?.let { value ->
                env[varName] = value
            }
        }
        
        // Add any custom environment variables provided
        env.putAll(environment)
        
        // Ensure essential environment variables are available
        env.putIfAbsent("PATH", System.getenv("PATH") ?: "")
        env.putIfAbsent("HOME", System.getenv("HOME") ?: System.getProperty("user.home"))
        env.putIfAbsent("USER", System.getenv("USER") ?: System.getProperty("user.name"))

        return try {
            val startTime = System.currentTimeMillis()
            val exitCode = executor.execute(commandLine, env)
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime

            val result = mapOf(
                "success" to (exitCode == 0),
                "exitCode" to exitCode,
                "duration" to duration,
                "command" to command,
                "workingDirectory" to workDir.absolutePath,
                "projectPath" to projectPath,
                "stdout" to outputStream.toString("UTF-8"),
                "stderr" to errorStream.toString("UTF-8")
            )

            objectMapper.writeValueAsString(result)
        } catch (e: ExecuteException) {
            val result = mapOf(
                "success" to false,
                "exitCode" to e.exitValue,
                "command" to command,
                "workingDirectory" to workDir.absolutePath,
                "projectPath" to projectPath,
                "stdout" to outputStream.toString("UTF-8"),
                "stderr" to errorStream.toString("UTF-8"),
                "error" to mapOf(
                    "type" to "execution",
                    "message" to "Command execution failed with exit code ${e.exitValue}"
                )
            )
            objectMapper.writeValueAsString(result)
        } catch (e: Exception) {
            when {
                e.message?.contains("timeout") == true -> {
                    createErrorResponse(
                        "Command execution timed out after ${timeout}ms",
                        "timeout"
                    )
                }
                e.message?.contains("No such file") == true -> {
                    createErrorResponse(
                        "Command not found: $command",
                        "not_found",
                        "Check if the command is installed and available in PATH"
                    )
                }
                else -> {
                    createErrorResponse(
                        "Execution failed: ${e.message}",
                        "execution"
                    )
                }
            }
        }
    }

    private fun createErrorResponse(
        message: String, 
        type: String = "error",
        suggestion: String? = null
    ): String {
        val error = mutableMapOf(
            "type" to type,
            "message" to message
        )
        if (suggestion != null) {
            error["suggestion"] = suggestion
        }

        val result = mapOf(
            "success" to false,
            "error" to error
        )
        return objectMapper.writeValueAsString(result)
    }

    private data class ValidationResult(
        val isValid: Boolean,
        val reason: String?
    )
}
