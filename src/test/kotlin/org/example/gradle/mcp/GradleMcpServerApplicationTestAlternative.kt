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
        "spring.main.web-application-type=none",
        "gradle.shell.enabled=false",
        "spring.ai.mcp.server.transports.stdio.enabled=false",
        "spring.ai.mcp.server.transports.sse.enabled=false"
    ]
)
class GradleMcpServerApplicationTestAlternative {

    @Test
    fun contextLoadsWithTestProperties() {
        // Test that the Spring context loads successfully with test properties
        // This approach uses configuration properties instead of mocking
    }
}
