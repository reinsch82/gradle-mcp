package org.example.gradle.mcp

/*
 * Mockito Configuration Notes
 * 
 * The GradleShellConfig is a Kotlin data class which creates final classes.
 * Mockito cannot mock final classes by default.
 * 
 * To enable mocking of final classes/data classes, you have these options:
 * 
 * 1. Use mockito-inline (already configured via mockito-extensions/org.mockito.plugins.MockMaker)
 * 
 * 2. Add this dependency to build.gradle.kts if needed:
 *    testImplementation("org.mockito:mockito-inline")
 * 
 * 3. Or open the data class for testing:
 *    Change: data class GradleShellConfig(...)
 *    To: open data class GradleShellConfig(...)
 * 
 * For this project, we use configuration-based testing instead of mocking
 * to avoid these complications.
 */

// Example of how to mock if needed in the future:
/*
import org.mockito.Mockito
import org.springframework.boot.test.mock.mockito.MockBean

@MockBean
private lateinit var gradleShellConfig: GradleShellConfig

// In test setup:
Mockito.`when`(gradleShellConfig.enabled).thenReturn(false)
*/
