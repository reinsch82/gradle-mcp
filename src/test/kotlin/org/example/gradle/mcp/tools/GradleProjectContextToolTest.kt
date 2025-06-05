package org.example.gradle.mcp.tools

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class GradleProjectContextToolTest {

    private lateinit var tool: GradleProjectContextTool
    private lateinit var objectMapper: ObjectMapper

    @TempDir
    private lateinit var tempDir: Path

    @BeforeEach
    fun setup() {
        tool = GradleProjectContextTool()
        objectMapper = jacksonObjectMapper()
        // Reset to default context
        GradleProjectContextTool.setCurrentProjectContext(System.getProperty("user.dir"))
    }

    @Test
    fun `should get current context`() {
        val arguments = objectMapper.createObjectNode().apply {
            put("action", "get")
        }

        val result = tool.execute(arguments)
        val response = objectMapper.readValue(result, Map::class.java) as Map<String, Any>

        assertTrue(response.containsKey("currentContext"))
        assertTrue(response.containsKey("exists"))
        assertTrue(response.containsKey("isDirectory"))
        assertNotNull(response["currentContext"])
    }

    @Test
    fun `should validate valid gradle project`() {
        // Create a simple gradle project in temp directory
        val buildFile = tempDir.resolve("build.gradle.kts").toFile()
        buildFile.writeText("// Test build file")

        val arguments = objectMapper.createObjectNode().apply {
            put("action", "validate")
            put("projectPath", tempDir.toString())
        }

        val result = tool.execute(arguments)
        val response = objectMapper.readValue(result, Map::class.java) as Map<String, Any>

        assertEquals(true, response["valid"])
        assertEquals(true, response["exists"])
        assertEquals(true, response["isDirectory"])
        assertEquals(true, response["isGradleProject"])
    }

    @Test
    fun `should validate non-gradle directory`() {
        val arguments = objectMapper.createObjectNode().apply {
            put("action", "validate")
            put("projectPath", tempDir.toString())
        }

        val result = tool.execute(arguments)
        val response = objectMapper.readValue(result, Map::class.java) as Map<String, Any>

        assertEquals(false, response["valid"])
        assertEquals(true, response["exists"])
        assertEquals(true, response["isDirectory"])
        assertEquals(false, response["isGradleProject"])
    }

    @Test
    fun `should change context to valid gradle project`() {
        // Create a gradle project
        val buildFile = tempDir.resolve("build.gradle").toFile()
        buildFile.writeText("apply plugin: 'java'")

        val originalContext = GradleProjectContextTool.getCurrentProjectContext()

        val arguments = objectMapper.createObjectNode().apply {
            put("action", "change")
            put("projectPath", tempDir.toString())
        }

        val result = tool.execute(arguments)
        val response = objectMapper.readValue(result, Map::class.java) as Map<String, Any>

        assertEquals(true, response["success"])
        assertEquals(originalContext, response["previousContext"])
        assertEquals(tempDir.toString(), response["newContext"])
        assertEquals(true, response["isGradleProject"])

        // Verify context was actually changed
        assertEquals(tempDir.toString(), GradleProjectContextTool.getCurrentProjectContext())

        // Restore original context
        GradleProjectContextTool.setCurrentProjectContext(originalContext)
    }

    @Test
    fun `should handle invalid project path`() {
        val invalidPath = "/this/path/does/not/exist"

        val arguments = objectMapper.createObjectNode().apply {
            put("action", "change")
            put("projectPath", invalidPath)
        }

        val result = tool.execute(arguments)
        val response = objectMapper.readValue(result, Map::class.java) as Map<String, Any>

        assertEquals(false, response["success"])
        assertTrue(response["error"].toString().contains("does not exist"))
    }

    @Test
    fun `should handle missing project path for change action`() {
        val arguments = objectMapper.createObjectNode().apply {
            put("action", "change")
        }

        val result = tool.execute(arguments)
        val response = objectMapper.readValue(result, Map::class.java) as Map<String, Any>

        assertEquals(false, response["success"])
        assertTrue(response["error"].toString().contains("required"))
    }

    @Test
    fun `should handle missing project path for validate action`() {
        val arguments = objectMapper.createObjectNode().apply {
            put("action", "validate")
        }

        val result = tool.execute(arguments)
        val response = objectMapper.readValue(result, Map::class.java) as Map<String, Any>

        assertEquals(false, response["valid"])
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

        @Suppress("UNCHECKED_CAST")
        val required = schema["required"] as List<String>
        assertTrue(required.contains("action"))
    }
}
