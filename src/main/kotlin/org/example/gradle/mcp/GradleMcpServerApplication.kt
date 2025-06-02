package org.example.gradle.mcp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class GradleMcpServerApplication

fun main(args: Array<String>) {
    runApplication<GradleMcpServerApplication>(*args)
}
