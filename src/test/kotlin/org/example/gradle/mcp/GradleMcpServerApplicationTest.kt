package org.example.gradle.mcp

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@TestPropertySource(properties = [
    "spring.ai.mcp.server.transports.stdio.enabled=false",
    "spring.ai.mcp.server.transports.sse.enabled=false"
])
class GradleMcpServerApplicationTest {

    @Test
    fun contextLoads() {
        // Test that the Spring context loads successfully
    }
}
