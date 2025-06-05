package org.example.gradle.mcp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.WebApplicationType

@SpringBootApplication
class GradleMcpServerApplication

fun main(args: Array<String>) {
    runApplication<GradleMcpServerApplication>(*args) {
        webApplicationType = WebApplicationType.NONE
        // Disable banner and startup logs for clean STDIO
        setBannerMode(org.springframework.boot.Banner.Mode.OFF)
        setLogStartupInfo(false)
    }
}
