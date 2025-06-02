package org.example.gradle.mcp.tools

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File

class GradleProjectContextToolTest {

    private lateinit var tool: GradleProjectContextTool
    private lateinit var objectMapper: ObjectMapper

    @TempDir
    private lateinit var tempDir: File

    @BeforeEach
    fun setup() {
        tool = GradleProjectContextTool()
        objectMapper = ObjectMapper()
    }

    @Test
    fun `should get current context`() {
        val result = tool.execute(objectMapper.readTree("""{"action": "get"}"""))
        val response: Map<String, Any> = objectMapper.readValue(result)
        
        assertTrue(response.containsKey("currentContext"))
        assertTrue(response.containsKey("exists"))
        assertTrue(response.containsKey("isDirectory"))
    }

    @Test
    fun `should validate valid gradle project`() {
        // Create a simple gradle project in temp directory
        val buildFile = File(tempDir, "build.gradle.kts")
        buildFile.writeText("// Test build file")
        
        val request = objectMapper.readTree("""
            {
                "action": "validate",
                "projectPath": "${tempDir.absolutePath}"
            }
        """.trimIndent())
        
        val result = tool.execute(request)
        val response: Map<String, Any> = objectMapper.readValue(result)
        
        assertEquals(true, response["valid"])
        assertEquals(true, response["exists"])
        assertEquals(true, response["isDirectory"])
        assertEquals(true, response["isGradleProject"])
    }

    @Test
    fun `should validate non-gradle directory`() {
        val request = objectMapper.readTree("""
            {
                "action": "validate",
                "projectPath": "${tempDir.absolutePath}"
            }
        """.trimIndent())
        
        val result = tool.execute(request)
        val response: Map<String, Any> = objectMapper.readValue(result)
        
        assertEquals(false, response["valid"])
        assertEquals(true, response["exists"])
        assertEquals(true, response["isDirectory"])
        assertEquals(false, response["isGradleProject"])
    }

    @Test
    fun `should change context to valid gradle project`() {
        // Create a gradle project
        val buildFile = File(tempDir, "build.gradle")
        buildFile.writeText("apply plugin: 'java'")
        
        val originalContext = GradleProjectContextTool.getCurrentProjectContext()
        
        val request = objectMapper.readTree("""
            {
                "action": "change",
                "projectPath": "${tempDir.absolutePath}"
            }
        """.trimIndent())
        
        val result = tool.execute(request)
        val response: Map<String, Any> = objectMapper.readValue(result)
        
        assertEquals(true, response["success"])
        assertEquals(originalContext, response["previousContext"])
        assertEquals(tempDir.absolutePath, response["newContext"])
        assertEquals(true, response["isGradleProject"])
        
        // Verify context was actually changed
        assertEquals(tempDir.absolutePath, GradleProjectContextTool.getCurrentProjectContext())
        
        // Restore original context
        GradleProjectContextTool.setCurrentProjectContext(originalContext)
    }

    @Test
    fun `should handle invalid project path`() {
        val invalidPath = "/this/path/does/not/exist"
        
        val request = objectMapper.readTree("""
            {
                "action": "change",
                "projectPath": "$invalidPath"
            }
        """.trimIndent())
        
        val result = tool.execute(request)
        val response: Map<String, Any> = objectMapper.readValue(result)
        
        assertEquals(false, response["success"])
        assertTrue(response["error"].toString().contains("does not exist"))
    }

    @Test
    fun `should handle missing project path for change action`() {
        val request = objectMapper.readTree("""{"action": "change"}""")
        
        val result = tool.execute(request)
        val response: Map<String, Any> = objectMapper.readValue(result)
        
        assertEquals(false, response["success"])
        assertTrue(response["error"].toString().contains("required"))
    }

    @Test
    fun `should have correct tool description and schema`() {
        val description = tool.getDescription()
        assertFalse(description.isBlank())
        assertTrue(description.contains("project context"))
        
        val schema = tool.getInputSchema()
        assertEquals("object", schema["type"])
        assertTrue(schema.containsKey("properties"))
        
        @Suppress("UNCHECKED_CAST")
        val properties = schema["properties"] as Map<String, Any>
        assertTrue(properties.containsKey("action"))
        assertTrue(properties.containsKey("projectPath"))
    }
}
