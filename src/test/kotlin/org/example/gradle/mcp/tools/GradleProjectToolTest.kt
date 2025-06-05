package org.example.gradle.mcp.tools

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File
import java.nio.file.Path

class GradleProjectToolTest {

    private lateinit var tool: GradleProjectTool
    private lateinit var objectMapper: ObjectMapper

    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setUp() {
        tool = GradleProjectTool()
        objectMapper = jacksonObjectMapper()
    }

    @Test
    fun `should execute with valid project path`() {
        // Create a basic gradle project structure
        val buildFile = tempDir.resolve("build.gradle.kts").toFile()
        buildFile.writeText("""
            plugins {
                kotlin("jvm")
            }
        """.trimIndent())

        val arguments = objectMapper.createObjectNode().apply {
            put("projectPath", tempDir.toString())
        }

        assertDoesNotThrow {
            val result = tool.execute(arguments)
            assertNotNull(result)
            assertFalse(result.isBlank())
            // Should contain project information
            assertTrue(result.contains("project") || result.contains("gradle") || result.contains("build"))
        }
    }

    @Test
    fun `should execute with current directory when no project path provided`() {
        val arguments = objectMapper.createObjectNode()

        assertDoesNotThrow {
            val result = tool.execute(arguments)
            assertNotNull(result)
            assertFalse(result.isBlank())
        }
    }

    @Test
    fun `should handle non-existent project path gracefully`() {
        val arguments = objectMapper.createObjectNode().apply {
            put("projectPath", "/non/existent/path")
        }

        assertDoesNotThrow {
            val result = tool.execute(arguments)
            assertNotNull(result)
            // Should return error message, not throw exception
            assertTrue(result.contains("does not exist") || result.contains("error") || result.contains("not found"))
        }
    }

    @Test
    fun `should handle null arguments gracefully`() {
        assertDoesNotThrow {
            val result = tool.execute(null)
            assertNotNull(result)
            assertFalse(result.isBlank())
        }
    }

    @Test
    fun `should have correct tool description and schema`() {
        val description = tool.getDescription()
        assertNotNull(description)
        assertFalse(description.isBlank())
        assertTrue(description.contains("project") || description.contains("Gradle"))

        val schema = tool.getInputSchema()
        assertNotNull(schema)
        assertEquals("object", schema["type"])
        assertTrue(schema.containsKey("properties"))

        @Suppress("UNCHECKED_CAST")
        val properties = schema["properties"] as Map<String, Any>
        assertTrue(properties.containsKey("projectPath"))
    }

    @Test
    fun `should handle empty project directory`() {
        val arguments = objectMapper.createObjectNode().apply {
            put("projectPath", tempDir.toString())
        }

        assertDoesNotThrow {
            val result = tool.execute(arguments)
            assertNotNull(result)
            // Even empty directory should be handled gracefully
            assertTrue(result.contains("project") || result.contains("directory") || result.contains("empty"))
        }
    }
}
