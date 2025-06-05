package org.example.gradle.mcp

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    classes = [GradleMcpServerApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = [
        "spring.main.web-application-type=none",
        "gradle.shell.enabled=false"
    ]
)
class MinimalContextTest {

    @Test
    fun `minimal context loads`() {
        // Minimal test to verify basic Spring context loading
        // Should work now that we fixed the bean conflict
    }
}
