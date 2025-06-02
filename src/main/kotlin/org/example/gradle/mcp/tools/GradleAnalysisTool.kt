package org.example.gradle.mcp.tools

import org.example.gradle.mcp.protocol.GradleTool
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import java.io.File

@Component
class GradleAnalysisTool : GradleTool {

    private val objectMapper = ObjectMapper()

    override fun getDescription(): String {
        return "Perform various analyses on Gradle projects including build script analysis, plugin usage, and project health checks."
    }

    override fun getInputSchema(): Map<String, Any> {
        return mapOf(
            "type" to "object",
            "properties" to mapOf(
                "projectPath" to mapOf(
                    "type" to "string",
                    "description" to "Path to the Gradle project directory (defaults to current context)"
                ),
                "analysisType" to mapOf(
                    "type" to "string",
                    "description" to "Type of analysis: overview, plugins, buildscript, health",
                    "default" to "overview"
                )
            )
        )
    }

    override fun execute(arguments: JsonNode?): String {
        val projectPath = arguments?.get("projectPath")?.asText() 
            ?: GradleProjectContextTool.getCurrentProjectContext()
        val analysisType = arguments?.get("analysisType")?.asText() ?: "overview"
        
        return try {
            when (analysisType) {
                "overview" -> analyzeProjectOverview(projectPath)
                "plugins" -> analyzePlugins(projectPath)
                "buildscript" -> analyzeBuildScript(projectPath)
                "health" -> analyzeProjectHealth(projectPath)
                else -> "Unknown analysis type: $analysisType. Supported types: overview, plugins, buildscript, health"
            }
        } catch (e: Exception) {
            "Error performing analysis: ${e.message}"
        }
    }

    private fun analyzeProjectOverview(projectPath: String): String {
        val projectDir = File(projectPath)
        
        if (!projectDir.exists()) {
            return objectMapper.writeValueAsString(mapOf(
                "success" to false,
                "error" to "Project directory does not exist: $projectPath"
            ))
        }

        val analysis = mutableMapOf<String, Any>()
        analysis["projectPath"] = projectPath
        analysis["projectName"] = projectDir.name
        analysis["isGradleProject"] = isGradleProject(projectDir)
        
        if (!isGradleProject(projectDir)) {
            analysis["error"] = "Not a Gradle project"
            return objectMapper.writeValueAsString(analysis)
        }

        analysis["structure"] = analyzeProjectStructure(projectDir)
        analysis["buildFiles"] = analyzeBuildFiles(projectDir)
        analysis["gradleVersion"] = detectGradleVersion(projectDir)
        
        return objectMapper.writeValueAsString(analysis)
    }

    private fun analyzePlugins(projectPath: String): String {
        val projectDir = File(projectPath)
        val buildFiles = findBuildFiles(projectDir)
        val pluginAnalysis = mutableMapOf<String, Any>()
        
        val allPlugins = mutableSetOf<String>()
        val pluginsByFile = mutableMapOf<String, List<String>>()
        
        buildFiles.forEach { buildFile ->
            val file = File(buildFile["path"] as String)
            if (file.exists()) {
                val content = file.readText()
                val plugins = extractPlugins(content, file.name.endsWith(".kts"))
                pluginsByFile[buildFile["name"] as String] = plugins
                allPlugins.addAll(plugins)
            }
        }
        
        pluginAnalysis["totalPlugins"] = allPlugins.size
        pluginAnalysis["uniquePlugins"] = allPlugins.toList().sorted()
        pluginAnalysis["pluginsByFile"] = pluginsByFile
        
        return objectMapper.writeValueAsString(pluginAnalysis)
    }

    private fun analyzeBuildScript(projectPath: String): String {
        val projectDir = File(projectPath)
        val buildFiles = findBuildFiles(projectDir)
        val analysis = mutableMapOf<String, Any>()
        
        buildFiles.forEach { buildFileInfo ->
            val file = File(buildFileInfo["path"] as String)
            if (file.exists()) {
                val content = file.readText()
                val fileName = buildFileInfo["name"] as String
                
                analysis[fileName] = mapOf(
                    "lines" to content.lines().size,
                    "size" to file.length(),
                    "hasRepositories" to content.contains("repositories"),
                    "hasDependencies" to content.contains("dependencies"),
                    "hasPlugins" to (content.contains("plugins") || content.contains("apply plugin")),
                    "hasKotlinDsl" to fileName.endsWith(".kts")
                )
            }
        }
        
        return objectMapper.writeValueAsString(analysis)
    }

    private fun analyzeProjectHealth(projectPath: String): String {
        val projectDir = File(projectPath)
        val health = mutableMapOf<String, Any>()
        val issues = mutableListOf<String>()
        val recommendations = mutableListOf<String>()
        
        if (!File(projectDir, "gradlew").exists()) {
            issues.add("Missing Gradle wrapper")
            recommendations.add("Add Gradle wrapper with 'gradle wrapper' command")
        }
        
        val gitignore = File(projectDir, ".gitignore")
        if (!gitignore.exists()) {
            issues.add("Missing .gitignore file")
            recommendations.add("Add .gitignore file to exclude build artifacts")
        }
        
        health["overallScore"] = calculateHealthScore(issues.size)
        health["issues"] = issues
        health["recommendations"] = recommendations
        health["issueCount"] = issues.size
        
        return objectMapper.writeValueAsString(health)
    }

    // Helper methods
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
                        "path" to file.absolutePath
                    ))
                }
            }
        
        return buildFiles
    }

    private fun analyzeProjectStructure(projectDir: File): Map<String, Any> {
        val structure = mutableMapOf<String, Any>()
        
        var dirCount = 0
        var fileCount = 0
        
        projectDir.walkTopDown().forEach { file ->
            if (file.isDirectory) dirCount++ else fileCount++
        }
        
        structure["totalDirectories"] = dirCount
        structure["totalFiles"] = fileCount
        structure["hasMultiProject"] = File(projectDir, "settings.gradle").exists() || File(projectDir, "settings.gradle.kts").exists()
        
        return structure
    }

    private fun analyzeBuildFiles(projectDir: File): Map<String, Any> {
        val analysis = mutableMapOf<String, Any>()
        val buildFiles = findBuildFiles(projectDir)
        
        analysis["count"] = buildFiles.size
        analysis["files"] = buildFiles
        analysis["hasKotlinDsl"] = buildFiles.any { (it["name"] as String).endsWith(".kts") }
        analysis["hasGroovyDsl"] = buildFiles.any { !(it["name"] as String).endsWith(".kts") }
        
        return analysis
    }

    private fun detectGradleVersion(projectDir: File): String {
        val wrapperProps = File(projectDir, "gradle/wrapper/gradle-wrapper.properties")
        if (wrapperProps.exists()) {
            val content = wrapperProps.readText()
            val versionRegex = "gradle-([0-9.]+)-".toRegex()
            val match = versionRegex.find(content)
            return match?.groupValues?.get(1) ?: "unknown"
        }
        return "unknown"
    }

    private fun extractPlugins(content: String, isKotlinDsl: Boolean): List<String> {
        val plugins = mutableListOf<String>()
        
        val pluginsBlockRegex = "plugins\\s*\\{([^}]*)\\}".toRegex(RegexOption.DOT_MATCHES_ALL)
        val pluginsMatch = pluginsBlockRegex.find(content)
        
        if (pluginsMatch != null) {
            val pluginsBlock = pluginsMatch.groupValues[1]
            
            if (isKotlinDsl) {
                val kotlinPluginRegex = "id\\s*\\(\\s*[\"']([^\"']+)[\"']\\s*\\)".toRegex()
                kotlinPluginRegex.findAll(pluginsBlock).forEach {
                    plugins.add(it.groupValues[1])
                }
            } else {
                val groovyPluginRegex = "id\\s+[\"']([^\"']+)[\"']".toRegex()
                groovyPluginRegex.findAll(pluginsBlock).forEach {
                    plugins.add(it.groupValues[1])
                }
            }
        }
        
        val applyPluginRegex = "apply\\s+plugin\\s*:\\s*[\"']([^\"']+)[\"']".toRegex()
        applyPluginRegex.findAll(content).forEach {
            plugins.add(it.groupValues[1])
        }
        
        return plugins.distinct()
    }

    private fun calculateHealthScore(issueCount: Int): String {
        return when {
            issueCount == 0 -> "Excellent"
            issueCount <= 2 -> "Good" 
            issueCount <= 5 -> "Fair"
            else -> "Poor"
        }
    }
}
