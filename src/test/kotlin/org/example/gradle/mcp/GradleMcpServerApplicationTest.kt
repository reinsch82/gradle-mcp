package org.example.gradle.mcp

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource

@SpringBootTest(
    classes = [GradleMcpServerApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@TestPropertySource(
    properties = [
        "gradle.shell.enabled=false",
        "spring.main.web-application-type=none",
        "spring.ai.mcp.server.transports.stdio.enabled=false",
        "spring.ai.mcp.server.transports.sse.enabled=false"
    ]
)
class GradleMcpServerApplicationTest {

    @Test
    fun contextLoads() {
        // Test that the Spring context loads successfully
        // This test verifies:
        // 1. Spring Boot application context can start
        // 2. All beans can be created without conflicts
        // 3. Configuration properties are properly bound
        // 4. No web server starts (webEnvironment = NONE)
        // 5. Shell tool is disabled for testing
    }
}
