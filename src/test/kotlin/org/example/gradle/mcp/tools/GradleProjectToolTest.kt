package org.example.gradle.mcp.tools

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import com.fasterxml.jackson.databind.ObjectMapper

class GradleProjectToolTest {

    private val tool = GradleProjectTool()
    private val objectMapper = ObjectMapper()

    @Test
    fun `should execute without throwing exception`() {
        val arguments = objectMapper.createObjectNode().apply {
            put("projectPath", System.getProperty("user.dir"))
        }

        assertDoesNotThrow {
            tool.execute(arguments)
        }
    }

    @Test
    fun `should handle non-existent project path gracefully`() {
        val arguments = objectMapper.createObjectNode().apply {
            put("projectPath", "/non/existent/path")
        }

        assertDoesNotThrow {
            val result = tool.execute(arguments)
            // Should return error message, not throw exception
            assert(result.contains("does not exist"))
        }
    }

    @Test
    fun `should handle null arguments gracefully`() {
        assertDoesNotThrow {
            tool.execute(null)
        }
    }
}
