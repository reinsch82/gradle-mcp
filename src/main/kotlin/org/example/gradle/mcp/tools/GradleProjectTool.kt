package org.example.gradle.mcp.tools

import org.example.gradle.mcp.protocol.GradleTool
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import java.io.File

@Component
class GradleProjectTool : GradleTool {

    private val objectMapper = ObjectMapper()

    override fun getDescription(): String {
        return "Get information about a Gradle project including basic structure, build files, and project properties"
    }

    override fun getInputSchema(): Map<String, Any> {
        return mapOf(
            "type" to "object",
            "properties" to mapOf(
                "projectPath" to mapOf(
                    "type" to "string",
                    "description" to "Path to the Gradle project directory (defaults to current context)"
                )
            )
        )
    }

    override fun execute(arguments: JsonNode?): String {
        val projectPath = arguments?.get("projectPath")?.asText() 
            ?: GradleProjectContextTool.getCurrentProjectContext()
        
        return try {
            analyzeGradleProject(projectPath)
        } catch (e: Exception) {
            "Error analyzing Gradle project: ${e.message}"
        }
    }

    private fun analyzeGradleProject(projectPath: String): String {
        val projectDir = File(projectPath)
        
        if (!projectDir.exists()) {
            return "Project directory does not exist: $projectPath"
        }

        val result = mutableMapOf<String, Any>()
        
        // Basic project info
        result["projectPath"] = projectPath
        result["projectName"] = projectDir.name
        result["isGradleProject"] = isGradleProject(projectDir)
        
        if (!isGradleProject(projectDir)) {
            return objectMapper.writeValueAsString(result + ("error" to "Not a Gradle project"))
        }

        // Build files
        result["buildFiles"] = findBuildFiles(projectDir)
        
        // Project structure
        result["sourceDirectories"] = findSourceDirectories(projectDir)
        
        // Gradle wrapper info
        result["gradleWrapper"] = getGradleWrapperInfo(projectDir)
        
        // Settings
        result["settings"] = getSettingsInfo(projectDir)
        
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
                        "size" to file.length().toString(),
                        "lastModified" to file.lastModified().toString()
                    ))
                }
            }
        
        return buildFiles
    }

    private fun findSourceDirectories(projectDir: File): List<String> {
        val sourceDirs = mutableListOf<String>()
        val commonSourcePaths = listOf(
            "src/main/java",
            "src/main/kotlin",
            "src/main/groovy",
            "src/main/scala",
            "src/main/resources",
            "src/test/java",
            "src/test/kotlin",
            "src/test/groovy",
            "src/test/scala",
            "src/test/resources"
        )
        
        commonSourcePaths.forEach { path ->
            val sourceDir = File(projectDir, path)
            if (sourceDir.exists() && sourceDir.isDirectory) {
                sourceDirs.add(path)
            }
        }
        
        return sourceDirs
    }

    private fun getGradleWrapperInfo(projectDir: File): Map<String, Any> {
        val wrapperInfo = mutableMapOf<String, Any>()
        
        val gradlewFile = File(projectDir, "gradlew")
        val wrapperPropsFile = File(projectDir, "gradle/wrapper/gradle-wrapper.properties")
        
        wrapperInfo["hasWrapper"] = gradlewFile.exists() && wrapperPropsFile.exists()
        
        if (wrapperPropsFile.exists()) {
            try {
                val props = java.util.Properties()
                wrapperPropsFile.inputStream().use { props.load(it) }
                wrapperInfo["distributionUrl"] = props.getProperty("distributionUrl", "unknown")
                wrapperInfo["distributionBase"] = props.getProperty("distributionBase", "unknown")
                wrapperInfo["distributionPath"] = props.getProperty("distributionPath", "unknown")
            } catch (e: Exception) {
                wrapperInfo["error"] = "Could not read wrapper properties: ${e.message}"
            }
        }
        
        return wrapperInfo
    }

    private fun getSettingsInfo(projectDir: File): Map<String, Any> {
        val settingsInfo = mutableMapOf<String, Any>()
        
        val settingsFiles = listOf("settings.gradle", "settings.gradle.kts")
        val settingsFile = settingsFiles.map { File(projectDir, it) }.firstOrNull { it.exists() }
        
        if (settingsFile != null) {
            settingsInfo["settingsFile"] = settingsFile.name
            settingsInfo["path"] = settingsFile.absolutePath
            
            try {
                val content = settingsFile.readText()
                settingsInfo["hasIncludedProjects"] = content.contains("include")
                settingsInfo["hasPluginManagement"] = content.contains("pluginManagement")
                settingsInfo["hasDependencyResolutionManagement"] = content.contains("dependencyResolutionManagement")
            } catch (e: Exception) {
                settingsInfo["error"] = "Could not read settings file: ${e.message}"
            }
        } else {
            settingsInfo["hasSettingsFile"] = false
        }
        
        return settingsInfo
    }
}
