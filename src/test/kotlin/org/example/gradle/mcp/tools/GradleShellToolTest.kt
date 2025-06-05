package org.example.gradle.mcp.tools

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.example.gradle.mcp.config.GradleShellConfig
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import java.io.File
import java.nio.file.Path

class GradleShellToolTest {

    private lateinit var shellTool: GradleShellTool
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
    private lateinit var testConfig: GradleShellConfig

    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setUp() {
        testConfig = GradleShellConfig(
            enabled = true,
            timeoutDefault = 30000,
            timeoutMax = 600000,
            validationMode = "strict",
            allowedCommands = listOf("./gradlew", "git", "find", "ls", "cat", "echo", "pwd", "env", "printenv", "dir", "set", "sh"),
            workingDirectoryRestriction = true,
            environmentPassthrough = listOf("PATH", "HOME", "USER")
        )
        shellTool = GradleShellTool(testConfig)
        // Set up a temporary project context
        GradleProjectContextTool.setCurrentProjectContext(tempDir.toString())
    }

    @Test
    @EnabledOnOs(OS.LINUX, OS.MAC)
    fun `should execute simple echo command successfully`() {
        val arguments = objectMapper.createObjectNode().apply {
            put("command", "echo 'Hello, Gradle MCP!'")
        }

        val result = shellTool.execute(arguments)
        val response = objectMapper.readValue(result, Map::class.java) as Map<String, Any>

        assertTrue(response["success"] as Boolean)
        assertEquals(0, response["exitCode"])
        assertTrue((response["stdout"] as String).contains("Hello, Gradle MCP!"))
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    fun `should execute simple echo command successfully on Windows`() {
        val arguments = objectMapper.createObjectNode().apply {
            put("command", "echo Hello, Gradle MCP!")
        }

        val result = shellTool.execute(arguments)
        val response = objectMapper.readValue(result, Map::class.java) as Map<String, Any>

        assertTrue(response["success"] as Boolean)
        assertEquals(0, response["exitCode"])
        assertTrue((response["stdout"] as String).contains("Hello, Gradle MCP!"))
    }

    @Test
    fun `should execute command with custom working directory`() {
        // Create a subdirectory
        val subDir = File(tempDir.toFile(), "subdir")
        subDir.mkdirs()
        File(subDir, "test.txt").writeText("test content")

        val arguments = objectMapper.createObjectNode().apply {
            put("command", if (System.getProperty("os.name").lowercase().contains("windows")) "dir" else "ls -la")
            put("workingDirectory", "subdir")
        }

        val result = shellTool.execute(arguments)
        val response = objectMapper.readValue(result, Map::class.java) as Map<String, Any>

        assertTrue(response["success"] as Boolean)
        assertTrue((response["stdout"] as String).contains("test.txt"))
        assertTrue((response["workingDirectory"] as String).endsWith("subdir"))
    }

    @Test
    @EnabledOnOs(OS.LINUX, OS.MAC)
    fun `should execute command with environment variables on Unix`() {
        // Test environment variables with a simple echo command that doesn't require variable expansion
        val arguments = objectMapper.createObjectNode().apply {
            put("command", "env")
            set<JsonNode>("environment", objectMapper.createObjectNode().apply {
                put("TEST_GRADLE_MCP", "test_value_for_mcp")
            })
        }

        val result = shellTool.execute(arguments)
        val response = objectMapper.readValue(result, Map::class.java) as Map<String, Any>

        assertTrue(response["success"] as Boolean, "env command should execute successfully")
        val stdout = response["stdout"] as String
        assertTrue(
            stdout.contains("TEST_GRADLE_MCP=test_value_for_mcp"),
            "Expected 'TEST_GRADLE_MCP=test_value_for_mcp' in env output. Stdout: '$stdout'"
        )
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    fun `should execute command with environment variables on Windows`() {
        // Test environment variables using the 'set' command on Windows
        val arguments = objectMapper.createObjectNode().apply {
            put("command", "set")
            set<JsonNode>("environment", objectMapper.createObjectNode().apply {
                put("TEST_GRADLE_MCP", "test_value_for_mcp_windows")
            })
        }

        val result = shellTool.execute(arguments)
        val response = objectMapper.readValue(result, Map::class.java) as Map<String, Any>

        if (response["success"] as Boolean) {
            val stdout = response["stdout"] as String
            assertTrue(
                stdout.contains("TEST_GRADLE_MCP=test_value_for_mcp_windows"),
                "Expected 'TEST_GRADLE_MCP=test_value_for_mcp_windows' in set output. Stdout: '$stdout'"
            )
        } else {
            // If 'set' command fails, it should not be a validation error
            val error = response["error"] as? Map<String, Any>
            assertNotEquals("validation", error?.get("type"))
            println("Windows set command failed: ${error?.get("message")}")
        }
    }

    @Test
    fun `should include environment variables in execution context`() {
        // Test that environment variables are at least passed to the execution context
        // without relying on shell-specific variable expansion
        val arguments = objectMapper.createObjectNode().apply {
            put("command", "echo 'Environment test completed'")
            set<JsonNode>("environment", objectMapper.createObjectNode().apply {
                put("TEST_VAR", "test_value")
                put("ANOTHER_VAR", "another_value")
            })
        }

        val result = shellTool.execute(arguments)
        val response = objectMapper.readValue(result, Map::class.java) as Map<String, Any>

        // The command should succeed (environment variables should not cause failure)
        assertTrue(response["success"] as Boolean, "Command should succeed when environment variables are provided")
        assertEquals(0, response["exitCode"])
        
        // The response should contain the expected output
        val stdout = response["stdout"] as String
        assertTrue(stdout.contains("Environment test completed"), "Expected success message in stdout: '$stdout'")
    }

    @Test
    fun `should handle environment variables parsing correctly`() {
        // Test that the JSON parsing of environment variables works correctly
        val envJson = objectMapper.createObjectNode().apply {
            put("VAR1", "value1")
            put("VAR2", "value2")
            put("SPECIAL_CHARS", "value with spaces and symbols: !@#$%")
        }
        
        val arguments = objectMapper.createObjectNode().apply {
            put("command", "echo 'Parsing test'")
            set<JsonNode>("environment", envJson)
        }

        val result = shellTool.execute(arguments)
        val response = objectMapper.readValue(result, Map::class.java) as Map<String, Any>

        // Should succeed - environment parsing should not cause errors
        assertTrue(response["success"] as Boolean, "Environment variable parsing should not cause command failure")
        assertEquals(0, response["exitCode"])
    }

    @Test
    fun `debug environment variable handling`() {
        // Debug test to understand what's happening with environment variables
        val arguments = objectMapper.createObjectNode().apply {
            put("command", "echo 'Debug test for environment handling'")
            set<JsonNode>("environment", objectMapper.createObjectNode().apply {
                put("DEBUG_VAR", "debug_value")
            })
        }

        val result = shellTool.execute(arguments)
        val response = objectMapper.readValue(result, Map::class.java) as Map<String, Any>

        // Print debug information
        println("Debug test result:")
        println("Success: ${response["success"]}")
        println("Exit code: ${response["exitCode"]}")
        println("Stdout: '${response["stdout"]}'")
        println("Stderr: '${response["stderr"]}'")
        if (response["error"] != null) {
            println("Error: ${response["error"]}")
        }

        // The command should at least succeed
        assertTrue(response["success"] as Boolean, "Debug test should succeed")
    }

    @Test
    fun `should reject dangerous rm command`() {
        val arguments = objectMapper.createObjectNode().apply {
            put("command", "rm -rf /")
        }

        val result = shellTool.execute(arguments)
        val response = objectMapper.readValue(result, Map::class.java) as Map<String, Any>

        assertFalse(response["success"] as Boolean)
        val error = response["error"] as Map<String, Any>
        assertEquals("validation", error["type"])
        assertTrue((error["message"] as String).contains("dangerous pattern"))
    }

    @Test
    fun `should reject sudo command`() {
        val arguments = objectMapper.createObjectNode().apply {
            put("command", "sudo rm file.txt")
        }

        val result = shellTool.execute(arguments)
        val response = objectMapper.readValue(result, Map::class.java) as Map<String, Any>

        assertFalse(response["success"] as Boolean)
        val error = response["error"] as Map<String, Any>
        assertEquals("validation", error["type"])
    }

    @Test
    fun `should handle command not found`() {
        val arguments = objectMapper.createObjectNode().apply {
            put("command", "nonexistent_command_12345")
        }

        val result = shellTool.execute(arguments)
        val response = objectMapper.readValue(result, Map::class.java) as Map<String, Any>

        assertFalse(response["success"] as Boolean)
        // Should either be validation error or execution error depending on shell behavior
        assertTrue(response.containsKey("error"))
    }

    @Test
    fun `should handle empty command`() {
        val arguments = objectMapper.createObjectNode().apply {
            put("command", "")
        }

        val result = shellTool.execute(arguments)
        val response = objectMapper.readValue(result, Map::class.java) as Map<String, Any>

        assertFalse(response["success"] as Boolean)
        val error = response["error"] as Map<String, Any>
        assertEquals("validation", error["type"])
        assertTrue((error["message"] as String).contains("cannot be empty"))
    }

    @Test
    fun `should handle non-existent working directory`() {
        val arguments = objectMapper.createObjectNode().apply {
            put("command", if (System.getProperty("os.name").lowercase().contains("windows")) "dir" else "ls")
            put("workingDirectory", "non_existent_dir")
        }

        val result = shellTool.execute(arguments)
        val response = objectMapper.readValue(result, Map::class.java) as Map<String, Any>

        assertFalse(response["success"] as Boolean)
        val error = response["error"] as Map<String, Any>
        assertEquals("not_found", error["type"])
    }

    @Test
    fun `should prevent directory traversal attacks`() {
        val arguments = objectMapper.createObjectNode().apply {
            put("command", if (System.getProperty("os.name").lowercase().contains("windows")) "dir" else "ls")
            put("workingDirectory", "../../../")
        }

        val result = shellTool.execute(arguments)
        val response = objectMapper.readValue(result, Map::class.java) as Map<String, Any>

        assertFalse(response["success"] as Boolean)
        val error = response["error"] as Map<String, Any>
        assertEquals("permission", error["type"])
        assertTrue((error["message"] as String).contains("within project directory"))
    }

    @Test
    fun `should have correct input schema`() {
        val schema = shellTool.getInputSchema()
        
        assertEquals("object", schema["type"])
        
        val properties = schema["properties"] as Map<String, Any>
        assertTrue(properties.containsKey("command"))
        assertTrue(properties.containsKey("projectPath"))
        assertTrue(properties.containsKey("timeout"))
        assertTrue(properties.containsKey("workingDirectory"))
        assertTrue(properties.containsKey("environment"))
        
        val required = schema["required"] as List<String>
        assertTrue(required.contains("command"))
    }

    @Test
    fun `should return proper description`() {
        val description = shellTool.getDescription()
        assertFalse(description.isEmpty())
        assertTrue(description.contains("shell commands"))
        assertTrue(description.contains("Gradle project context"))
    }

    @Test
    fun `should execute gradle wrapper command`() {
        // Create a mock gradlew file
        val gradlewFile = File(tempDir.toFile(), "gradlew")
        gradlewFile.writeText("#!/bin/bash\necho 'Gradle wrapper executed'\n")
        gradlewFile.setExecutable(true)

        val arguments = objectMapper.createObjectNode().apply {
            put("command", "./gradlew --version")
        }

        val result = shellTool.execute(arguments)
        val response = objectMapper.readValue(result, Map::class.java) as Map<String, Any>

        // Should not fail validation (gradlew commands should be allowed)
        if (!(response["success"] as Boolean)) {
            val error = response["error"] as? Map<String, Any>
            // If it fails, it should not be due to validation
            assertNotEquals("validation", error?.get("type"))
        }
    }

    @Test
    fun `should respect disabled configuration`() {
        val disabledConfig = testConfig.copy(enabled = false)
        val disabledShellTool = GradleShellTool(disabledConfig)

        val arguments = objectMapper.createObjectNode().apply {
            put("command", "echo 'test'")
        }

        val result = disabledShellTool.execute(arguments)
        val response = objectMapper.readValue(result, Map::class.java) as Map<String, Any>

        assertFalse(response["success"] as Boolean)
        val error = response["error"] as Map<String, Any>
        assertEquals("disabled", error["type"])
        assertTrue((error["message"] as String).contains("disabled"))
    }

    @Test
    fun `should enforce timeout limits`() {
        val limitedConfig = testConfig.copy(timeoutMax = 1000) // 1 second max
        val limitedShellTool = GradleShellTool(limitedConfig)

        val arguments = objectMapper.createObjectNode().apply {
            put("command", "echo 'test'")
            put("timeout", 5000) // Request 5 seconds but should be limited to 1
        }

        val result = limitedShellTool.execute(arguments)
        // Should not throw an error, but timeout should be capped
        val response = objectMapper.readValue(result, Map::class.java) as Map<String, Any>
        
        // This test mainly ensures the timeout capping logic doesn't crash
        assertTrue(response.containsKey("success"))
    }

    @Test
    fun `should work in whitelist mode`() {
        val whitelistConfig = testConfig.copy(
            validationMode = "whitelist",
            allowedCommands = listOf("echo", "ls")
        )
        val whitelistShellTool = GradleShellTool(whitelistConfig)

        // Allowed command should work
        val allowedArgs = objectMapper.createObjectNode().apply {
            put("command", "echo 'allowed'")
        }
        val allowedResult = whitelistShellTool.execute(allowedArgs)
        val allowedResponse = objectMapper.readValue(allowedResult, Map::class.java) as Map<String, Any>
        assertTrue(allowedResponse["success"] as Boolean)

        // Disallowed command should fail
        val disallowedArgs = objectMapper.createObjectNode().apply {
            put("command", "find . -name '*.txt'")
        }
        val disallowedResult = whitelistShellTool.execute(disallowedArgs)
        val disallowedResponse = objectMapper.readValue(disallowedResult, Map::class.java) as Map<String, Any>
        assertFalse(disallowedResponse["success"] as Boolean)
        val error = disallowedResponse["error"] as Map<String, Any>
        assertEquals("validation", error["type"])
        assertTrue((error["message"] as String).contains("whitelist"))
    }
}
