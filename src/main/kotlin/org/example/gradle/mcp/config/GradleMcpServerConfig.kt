package org.example.gradle.mcp.config

import org.example.gradle.mcp.protocol.McpServer
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
class GradleMcpServerConfig {

    @Bean
    @Profile("!test")
    fun mcpServerRunner(mcpServer: McpServer): CommandLineRunner {
        return CommandLineRunner {
            mcpServer.start()
        }
    }
}
