# Gradle Shell Tool Examples

The `gradle_shell` tool allows you to execute shell commands within the Gradle project context with built-in safety validation.

## Basic Usage

### Simple Commands

```json
{
  "command": "echo 'Hello from Gradle MCP!'"
}
```

```json
{
  "command": "pwd"
}
```

### Gradle Commands

```json
{
  "command": "./gradlew tasks --all"
}
```

```json
{
  "command": "./gradlew test --tests '*CheckstylePluginClasspathIntegrationTest*' --info",
  "timeout": 300000
}
```

### Git Operations

```json
{
  "command": "git log --oneline -10"
}
```

```json
{
  "command": "git status --porcelain"
}
```

## Advanced Usage

### Custom Working Directory

```json
{
  "command": "ls -la",
  "workingDirectory": "src/main/kotlin"
}
```

### Environment Variables

```json
{
  "command": "echo $CUSTOM_VAR && java -version",
  "environment": {
    "CUSTOM_VAR": "my_value",
    "JAVA_HOME": "/usr/lib/jvm/java-17-openjdk"
  }
}
```

### Complex Build Analysis

```json
{
  "command": "find . -name '*.gradle*' -exec grep -l 'kotlin' {} \\;",
  "workingDirectory": "platforms"
}
```

### Project Exploration

```json
{
  "command": "find . -type f -name '*.kt' | head -20"
}
```

## Use Cases Solved by gradle_shell

### 1. Running Tests with Complex Filters
The original problem that led to this feature:

```json
{
  "command": "./gradlew :code-quality:test --tests '*CheckstylePluginClasspathIntegrationTest*accepts*throwing*exception*' --stacktrace",
  "timeout": 300000
}
```

### 2. Debugging Build Issues

```json
{
  "command": "./gradlew build --debug --stacktrace",
  "timeout": 600000
}
```

### 3. Analyzing Project Structure

```json
{
  "command": "find . -name '*.gradle*' | xargs grep -l 'checkstyle' | head -10"
}
```

### 4. Git Integration

```json
{
  "command": "git diff --name-only HEAD~1..HEAD"
}
```

### 5. File System Operations

```json
{
  "command": "grep -r 'CheckstylePluginClasspathIntegrationTest' src/",
  "workingDirectory": "platforms/jvm/code-quality"
}
```

## Safety Features

### Command Validation
The tool includes three validation modes:

- **strict** (default): Blocks dangerous patterns like `rm -rf /`, `sudo`, etc.
- **permissive**: Only blocks extremely dangerous commands
- **whitelist**: Only allows pre-approved commands

### Working Directory Restriction
By default, working directories must be within the project to prevent directory traversal attacks.

### Environment Variable Control
Only configured environment variables are passed through to commands.

## Configuration

Configure the shell tool in `application.yml`:

```yaml
gradle:
  shell:
    enabled: true
    timeout-default: 30000
    timeout-max: 600000
    validation-mode: strict
    working-directory-restriction: true
    allowed-commands:
      - "./gradlew"
      - "git"
      - "find"
      - "ls"
      # ... more commands
    environment-passthrough:
      - "JAVA_HOME"
      - "PATH"
      # ... more variables
```

## Error Handling

The tool provides structured error responses:

```json
{
  "success": false,
  "error": {
    "type": "validation",
    "message": "Command contains potentially dangerous pattern: sudo",
    "suggestion": "Use commands that don't require elevated privileges"
  }
}
```

Error types:
- `validation`: Command failed safety validation
- `not_found`: Command or directory not found
- `permission`: Security restriction violated
- `timeout`: Command execution timed out
- `execution`: General execution error

## Response Format

Successful execution:

```json
{
  "success": true,
  "exitCode": 0,
  "duration": 1250,
  "command": "./gradlew tasks",
  "workingDirectory": "/path/to/project",
  "projectPath": "/path/to/project",
  "stdout": "Build tasks\n-----------\nassemble - Assembles...",
  "stderr": ""
}
```

Failed execution:

```json
{
  "success": false,
  "exitCode": 1,
  "command": "./gradlew nonexistent",
  "workingDirectory": "/path/to/project",
  "stdout": "",
  "stderr": "Task 'nonexistent' not found in root project"
}
```
