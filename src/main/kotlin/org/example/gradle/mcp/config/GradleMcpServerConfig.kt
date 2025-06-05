package org.example.gradle.mcp.config

import org.example.gradle.mcp.protocol.McpServer
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@EnableConfigurationProperties(GradleShellConfig::class)
class GradleMcpServerConfig {

    @Bean
    @Profile("!test")
    fun mcpServerRunner(mcpServer: McpServer): CommandLineRunner {
        return CommandLineRunner {
            mcpServer.start()
        }
    }
}

@ConfigurationProperties(prefix = "gradle.shell")
data class GradleShellConfig(
    var enabled: Boolean = true,
    var timeoutDefault: Long = 30000,
    var timeoutMax: Long = 600000,
    var validationMode: String = "strict", // "strict" | "permissive" | "whitelist"
    var allowedCommands: List<String> = listOf(
        "./gradlew", "gradlew", "gradle", "git", "find", "ls", "cat", "grep", "echo", "which", "whereis"
    ),
    var workingDirectoryRestriction: Boolean = true,
    var environmentPassthrough: List<String> = listOf("JAVA_HOME", "GRADLE_OPTS", "PATH", "HOME", "USER")
)
