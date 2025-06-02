# Gradle MCP Server

A Model Context Protocol (MCP) server implementation for Gradle project management and analysis. This server provides tools, resources, and prompts to help AI assistants interact with Gradle projects effectively.

## Features

### Tools

1. **gradle_project_info** - Get comprehensive information about a Gradle project
   - Project structure analysis
   - Build files detection
   - Source directories mapping
   - Gradle wrapper information

2. **gradle_build** - Execute Gradle build commands and tasks
   - Run any Gradle task (build, clean, test, etc.)
   - Capture build output and errors
   - Support for custom arguments and timeouts

3. **gradle_dependencies** - Analyze and manage project dependencies
   - List all dependencies
   - Generate dependency trees
   - Dependency insight analysis
   - Check for outdated dependencies (with plugins)
   - Security vulnerability scanning (with plugins)

4. **gradle_tasks** - List and analyze available Gradle tasks
   - List tasks by group
   - Show all available tasks
   - Get help for specific tasks
   - Parse task output for structured information

5. **gradle_analysis** - Perform various project analyses
   - Project overview and health checks
   - Plugin usage analysis
   - Build script complexity assessment
   - Performance optimization suggestions

### Resources

The server exposes various Gradle project resources:

- **Build Files** - Access to build.gradle, build.gradle.kts, settings.gradle, etc.
- **Configuration Files** - gradle.properties, wrapper properties
- **Project Structure** - JSON summary of project organization
- **Dependencies Summary** - Overview of project dependencies
- **Task Lists** - Available Gradle tasks

### Prompts

Pre-built prompts for common Gradle workflows:

- **gradle_project_analysis** - Comprehensive project analysis workflow
- **gradle_build_optimization** - Build performance optimization guide
- **gradle_troubleshooting** - Step-by-step troubleshooting assistance

## Setup and Usage

### Prerequisites

- Java 21 or higher
- Gradle 8.7+ (or use the wrapper)
- Spring AI MCP dependencies

### Building the Server

```bash
./gradlew build
```

### Running the Server

#### Standalone Mode
```bash
./gradlew bootRun
```

#### As MCP Server (STDIO)
```bash
java -jar build/libs/gradle-mcp-1.0-SNAPSHOT.jar
```

#### As MCP Server (HTTP SSE)
```bash
./gradlew bootRun
# Server will be available at http://localhost:8080/mcp/sse
```

### Configuration with Claude Desktop

Add to your Claude Desktop MCP configuration:

```json
{
  "mcpServers": {
    "gradle-mcp": {
      "command": "java",
      "args": ["-jar", "/path/to/gradle-mcp-1.0-SNAPSHOT.jar"],
      "cwd": "/path/to/your/gradle/project"
    }
  }
}
```

### Configuration with VS Code

Add to your VS Code MCP configuration:

```json
{
  "name": "Gradle MCP Server",
  "command": "java",
  "args": ["-jar", "/path/to/gradle-mcp-1.0-SNAPSHOT.jar"],
  "cwd": "/path/to/your/gradle/project"
}
```

## Tool Usage Examples

### Analyze Project Information
```json
{
  "tool": "gradle_project_info",
  "arguments": {
    "projectPath": "/path/to/gradle/project"
  }
}
```

### Run Build Task
```json
{
  "tool": "gradle_build",
  "arguments": {
    "projectPath": "/path/to/gradle/project",
    "task": "build",
    "args": "--info"
  }
}
```

### Analyze Dependencies
```json
{
  "tool": "gradle_dependencies",
  "arguments": {
    "projectPath": "/path/to/gradle/project",
    "action": "list",
    "configuration": "runtimeClasspath"
  }
}
```

### List Tasks
```json
{
  "tool": "gradle_tasks",
  "arguments": {
    "projectPath": "/path/to/gradle/project",
    "action": "list",
    "group": "build"
  }
}
```

### Project Analysis
```json
{
  "tool": "gradle_analysis",
  "arguments": {
    "projectPath": "/path/to/gradle/project",
    "analysisType": "overview"
  }
}
```

## Resource Access Examples

### Read Build File
```
gradle://buildfile/build.gradle.kts
```

### Get Project Structure
```
gradle://project/structure
```

### Dependencies Summary
```
gradle://dependencies/summary
```

## Development

### Project Structure
```
src/main/kotlin/org/example/gradle/mcp/
├── GradleMcpServerApplication.kt          # Main application
├── config/
│   └── GradleMcpServerConfig.kt          # MCP server configuration
├── tools/                                 # MCP tools implementation
│   ├── GradleProjectTool.kt              # Project info tool
│   ├── GradleBuildTool.kt                # Build execution tool
│   ├── GradleDependencyTool.kt           # Dependency analysis tool
│   ├── GradleTaskTool.kt                 # Task management tool
│   └── GradleAnalysisTool.kt             # Project analysis tool
├── resources/
│   └── GradleResourceProvider.kt         # Resource provider
└── prompts/
    └── GradlePromptProvider.kt           # Prompt templates
```

### Adding New Tools

1. Create a new tool class in the `tools` package
2. Annotate with `@Tool` and implement the required interface
3. Add the tool to the configuration in `GradleMcpServerConfig`

### Adding New Resources

1. Update `GradleResourceProvider` to include new resource types
2. Implement the `readResource` method for the new URI patterns

### Adding New Prompts

1. Add new prompt definitions to `GradlePromptProvider`
2. Implement the prompt generation logic

## License

This project is licensed under the MIT License.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## Support

For issues and questions:
1. Check the existing GitHub issues
2. Create a new issue with detailed information
3. Include project setup and error messages if applicable
