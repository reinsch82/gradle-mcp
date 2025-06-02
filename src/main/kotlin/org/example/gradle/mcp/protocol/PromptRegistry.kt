package org.example.gradle.mcp.protocol

import com.fasterxml.jackson.databind.JsonNode
import org.springframework.stereotype.Component

@Component
class PromptRegistry {

    fun listPrompts(): List<Map<String, Any>> {
        return listOf(
            mapOf(
                "name" to "gradle_project_analysis",
                "description" to "Analyze a Gradle project and provide insights about its structure, dependencies, and health"
            ),
            
            mapOf(
                "name" to "gradle_build_optimization",
                "description" to "Analyze a Gradle project and suggest build performance optimizations"
            ),
            
            mapOf(
                "name" to "gradle_troubleshooting",
                "description" to "Help troubleshoot common Gradle build issues and errors"
            )
        )
    }

    fun getPrompt(name: String, arguments: JsonNode?): String {
        return when (name) {
            "gradle_project_analysis" -> """
                Please analyze the current Gradle project and provide comprehensive insights.
                
                Use the available Gradle MCP tools to:
                1. Get project information with gradle_project_info
                2. Analyze project structure with gradle_analysis
                3. Review dependencies with gradle_dependencies  
                4. List available tasks with gradle_tasks
                5. Perform health checks
                
                Provide a detailed analysis with actionable recommendations.
            """.trimIndent()
            
            "gradle_build_optimization" -> """
                Please analyze the current Gradle project for build performance optimization.
                
                Focus on:
                1. Build configuration analysis
                2. Dependency optimization opportunities
                3. Task execution optimization
                4. Gradle version and plugin updates
                5. Performance settings recommendations
                
                Provide specific optimization recommendations with expected impact.
            """.trimIndent()
            
            "gradle_troubleshooting" -> """
                Please help troubleshoot Gradle build issues.
                
                Follow this approach:
                1. Gather project information
                2. Analyze build configuration
                3. Check for common issues
                4. Provide step-by-step solutions
                5. Suggest preventive measures
                
                Use the available Gradle tools to diagnose and resolve issues.
            """.trimIndent()
            
            else -> throw IllegalArgumentException("Unknown prompt: $name")
        }
    }
}
