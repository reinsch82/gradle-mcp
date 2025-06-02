package org.example.gradle.mcp.tools

import org.example.gradle.mcp.protocol.GradleTool
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import java.io.File

@Component
class GradleProjectContextTool : GradleTool {

    private val objectMapper = ObjectMapper()
    
    companion object {
        @Volatile
        private var currentProjectContext: String = System.getProperty("user.dir")
        
        fun getCurrentProjectContext(): String = currentProjectContext
        
        fun setCurrentProjectContext(path: String) {
            currentProjectContext = path
        }
    }

    override fun getDescription(): String {
        return "Change the current project context (working directory) for all subsequent Gradle operations. " +
                "This allows switching between different Gradle projects without restarting the MCP server."
    }

    override fun getInputSchema(): Map<String, Any> {
        return mapOf(
            "type" to "object",
            "properties" to mapOf(
                "projectPath" to mapOf(
                    "type" to "string",
                    "description" to "Path to the Gradle project directory to switch to"
                ),
                "action" to mapOf(
                    "type" to "string",
                    "description" to "Action to perform: 'change' to change context, 'get' to get current context, 'validate' to validate a path",
                    "enum" to listOf("change", "get", "validate"),
                    "default" to "change"
                )
            ),
            "required" to listOf("action")
        )
    }

    override fun execute(arguments: JsonNode?): String {
        val action = arguments?.get("action")?.asText() ?: "change"
        val projectPath = arguments?.get("projectPath")?.asText()
        
        return try {
            when (action) {
                "get" -> getCurrentContext()
                "validate" -> validateProjectPath(projectPath)
                "change" -> changeProjectContext(projectPath)
                else -> "Invalid action: $action. Valid actions are: change, get, validate"
            }
        } catch (e: Exception) {
            "Error executing project context operation: ${e.message}"
        }
    }

    private fun getCurrentContext(): String {
        val currentPath = getCurrentProjectContext()
        val projectDir = File(currentPath)
        val isGradleProject = isGradleProject(projectDir)
        
        val result = mapOf(
            "currentContext" to currentPath,
            "exists" to projectDir.exists(),
            "isDirectory" to projectDir.isDirectory,
            "isGradleProject" to isGradleProject,
            "projectName" to projectDir.name,
            "absolutePath" to projectDir.absolutePath
        )
        
        return objectMapper.writeValueAsString(result)
    }

    private fun validateProjectPath(projectPath: String?): String {
        if (projectPath == null) {
            return objectMapper.writeValueAsString(mapOf(
                "valid" to false,
                "error" to "Project path is required for validation"
            ))
        }
        
        val projectDir = File(projectPath)
        val exists = projectDir.exists()
        val isDirectory = projectDir.isDirectory
        val isGradleProject = if (exists && isDirectory) isGradleProject(projectDir) else false
        val absolutePath = projectDir.absolutePath
        
        val result = mutableMapOf<String, Any>(
            "projectPath" to projectPath,
            "absolutePath" to absolutePath,
            "exists" to exists,
            "isDirectory" to isDirectory,
            "isGradleProject" to isGradleProject,
            "valid" to (exists && isDirectory && isGradleProject),
            "projectName" to projectDir.name
        )
        
        if (exists && isDirectory && !isGradleProject) {
            result["warning"] = "Directory exists but does not appear to be a Gradle project"
        }
        
        return objectMapper.writeValueAsString(result)
    }

    private fun changeProjectContext(projectPath: String?): String {
        if (projectPath == null) {
            return objectMapper.writeValueAsString(mapOf(
                "success" to false,
                "error" to "Project path is required"
            ))
        }
        
        val projectDir = File(projectPath)
        
        if (!projectDir.exists()) {
            return objectMapper.writeValueAsString(mapOf(
                "success" to false,
                "error" to "Project directory does not exist: $projectPath"
            ))
        }
        
        if (!projectDir.isDirectory) {
            return objectMapper.writeValueAsString(mapOf(
                "success" to false,
                "error" to "Path is not a directory: $projectPath"
            ))
        }
        
        val isGradleProject = isGradleProject(projectDir)
        val absolutePath = projectDir.absolutePath
        val previousContext = getCurrentProjectContext()
        
        // Update the context
        setCurrentProjectContext(absolutePath)
        
        val result = mutableMapOf<String, Any>(
            "success" to true,
            "previousContext" to previousContext,
            "newContext" to absolutePath,
            "projectName" to projectDir.name,
            "isGradleProject" to isGradleProject
        )
        
        if (!isGradleProject) {
            result["warning"] = "Directory does not appear to be a Gradle project (no build.gradle or build.gradle.kts found)"
        } else {
            // Gather some basic project info
            val buildFiles = findBuildFiles(projectDir)
            result["buildFiles"] = buildFiles.map { it["name"]!! }
            
            // Check for multi-project setup
            val settingsFile = findSettingsFile(projectDir)
            if (settingsFile != null) {
                result["hasSettings"] = true
                result["settingsFile"] = settingsFile.name
                
                try {
                    val content = settingsFile.readText()
                    result["isMultiProject"] = content.contains("include")
                } catch (e: Exception) {
                    // Ignore read errors
                }
            }
        }
        
        return objectMapper.writeValueAsString(result)
    }

    private fun isGradleProject(projectDir: File): Boolean {
        return listOf("build.gradle", "build.gradle.kts", "settings.gradle", "settings.gradle.kts")
            .any { File(projectDir, it).exists() }
    }

    private fun findBuildFiles(projectDir: File): List<Map<String, String>> {
        val buildFiles = mutableListOf<Map<String, String>>()
        
        listOf("build.gradle", "build.gradle.kts", "settings.gradle", "settings.gradle.kts")
            .forEach { fileName ->
                val file = File(projectDir, fileName)
                if (file.exists()) {
                    buildFiles.add(mapOf(
                        "name" to fileName,
                        "path" to file.absolutePath,
                        "size" to file.length().toString()
                    ))
                }
            }
        
        return buildFiles
    }

    private fun findSettingsFile(projectDir: File): File? {
        return listOf("settings.gradle", "settings.gradle.kts")
            .map { File(projectDir, it) }
            .firstOrNull { it.exists() }
    }
}
