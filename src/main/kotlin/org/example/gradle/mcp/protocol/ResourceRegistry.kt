package org.example.gradle.mcp.protocol

import com.fasterxml.jackson.databind.ObjectMapper
import org.example.gradle.mcp.tools.GradleProjectContextTool
import org.springframework.stereotype.Component
import java.io.File

@Component
class ResourceRegistry {

    private val objectMapper = ObjectMapper()

    fun listResources(): List<Map<String, Any>> {
        val projectPath = GradleProjectContextTool.getCurrentProjectContext()
        val projectDir = File(projectPath)
        
        val resources = mutableListOf<Map<String, Any>>()
        
        // Add build files as resources
        val buildFiles = listOf("build.gradle", "build.gradle.kts", "settings.gradle", "settings.gradle.kts")
        buildFiles.forEach { fileName ->
            val file = File(projectDir, fileName)
            if (file.exists()) {
                resources.add(mapOf(
                    "uri" to "gradle://buildfile/$fileName",
                    "name" to "Gradle Build File: $fileName",
                    "description" to "Gradle build configuration file",
                    "mimeType" to if (fileName.endsWith(".kts")) "text/kotlin" else "text/groovy"
                ))
            }
        }
        
        // Add project structure summary
        resources.add(mapOf(
            "uri" to "gradle://project/structure",
            "name" to "Project Structure Summary",
            "description" to "Overview of Gradle project structure and files",
            "mimeType" to "application/json"
        ))
        
        return resources
    }

    fun readResource(uri: String): String {
        val projectPath = GradleProjectContextTool.getCurrentProjectContext()
        val projectDir = File(projectPath)
        
        return when {
            uri.startsWith("gradle://buildfile/") -> {
                val fileName = uri.substring("gradle://buildfile/".length)
                val file = File(projectDir, fileName)
                if (file.exists()) {
                    file.readText()
                } else {
                    throw IllegalArgumentException("Build file not found: $fileName")
                }
            }
            
            uri == "gradle://project/structure" -> {
                generateProjectStructureSummary(projectDir)
            }
            
            else -> throw IllegalArgumentException("Unsupported resource URI: $uri")
        }
    }

    private fun generateProjectStructureSummary(projectDir: File): String {
        val summary = mutableMapOf<String, Any>()
        
        summary["projectName"] = projectDir.name
        summary["projectPath"] = projectDir.absolutePath
        summary["isGradleProject"] = isGradleProject(projectDir)
        
        return objectMapper.writeValueAsString(summary)
    }

    private fun isGradleProject(projectDir: File): Boolean {
        return listOf("build.gradle", "build.gradle.kts", "settings.gradle", "settings.gradle.kts")
            .any { File(projectDir, it).exists() }
    }
}
