# Gradle Project Context Tool

This document describes the new `gradle_project_context` tool that has been added to the Gradle MCP server.

## Overview

The Project Context Tool allows you to change the working directory context for all Gradle operations without restarting the MCP server. This is particularly useful when working with multiple Gradle projects or when you need to switch between different project contexts during a session.

## Features

- **Context Switching**: Change the current project context to any valid directory
- **Context Validation**: Validate if a directory is a valid Gradle project before switching
- **Context Information**: Get detailed information about the current project context
- **Seamless Integration**: All other Gradle tools automatically use the current context

## Tool Registration

The tool is registered in the `ToolRegistry` with the name `gradle_project_context`.

## Actions

### 1. Change Context (`change`)

Changes the current project context to the specified directory.

**Parameters:**
- `action`: "change" (required)
- `projectPath`: Path to the Gradle project directory (required)

**Example:**
```json
{
  "action": "change",
  "projectPath": "/path/to/my/gradle/project"
}
```

**Response:**
```json
{
  "success": true,
  "previousContext": "/previous/path",
  "newContext": "/path/to/my/gradle/project",
  "projectName": "my-project",
  "isGradleProject": true,
  "buildFiles": ["build.gradle.kts", "settings.gradle.kts"],
  "hasSettings": true,
  "settingsFile": "settings.gradle.kts",
  "isMultiProject": false
}
```

### 2. Get Current Context (`get`)

Retrieves information about the current project context.

**Parameters:**
- `action`: "get" (required)

**Example:**
```json
{
  "action": "get"
}
```

**Response:**
```json
{
  "currentContext": "/current/project/path",
  "exists": true,
  "isDirectory": true,
  "isGradleProject": true,
  "projectName": "current-project",
  "absolutePath": "/absolute/current/project/path"
}
```

### 3. Validate Path (`validate`)

Validates whether a given path is a valid Gradle project without changing the context.

**Parameters:**
- `action`: "validate" (required)
- `projectPath`: Path to validate (required)

**Example:**
```json
{
  "action": "validate",
  "projectPath": "/path/to/validate"
}
```

**Response:**
```json
{
  "projectPath": "/path/to/validate",
  "absolutePath": "/absolute/path/to/validate",
  "exists": true,
  "isDirectory": true,
  "isGradleProject": true,
  "valid": true,
  "projectName": "validated-project"
}
```

## Integration with Other Tools

Once you set a project context, all other Gradle tools will use this context as their default:

- `gradle_project_info`: Will analyze the current context project
- `gradle_build`: Will execute builds in the current context
- `gradle_tasks`: Will list tasks from the current context project
- `gradle_dependencies`: Will analyze dependencies in the current context
- `gradle_analysis`: Will perform analysis on the current context project

You can still override the context for individual tool calls by providing the `projectPath` parameter.

## Error Handling

The tool provides comprehensive error handling:

- **Invalid paths**: Reports when a path doesn't exist
- **Non-directories**: Reports when a path exists but isn't a directory
- **Non-Gradle projects**: Warns when a directory exists but isn't a Gradle project
- **Missing parameters**: Reports missing required parameters

## Thread Safety

The context management is thread-safe using the `@Volatile` annotation on the context variable and companion object methods.

## Implementation Details

### Context Storage

The current context is stored in a companion object with:
- `getCurrentProjectContext()`: Returns the current context path
- `setCurrentProjectContext(path: String)`: Updates the context path

### Default Behavior

- Initial context is set to `System.getProperty("user.dir")` (current working directory)
- All tools fall back to the context when no explicit `projectPath` is provided
- Context persists across multiple tool calls within the same session

### Project Detection

A directory is considered a Gradle project if it contains any of:
- `build.gradle`
- `build.gradle.kts`
- `settings.gradle`
- `settings.gradle.kts`

## Usage Examples

### Switching Between Projects

```json
// Check current context
{
  "action": "get"
}

// Switch to a different project
{
  "action": "change",
  "projectPath": "/workspace/project-a"
}

// Now all gradle tools operate on project-a by default
// Run build on the current context
{
  "task": "build"
}

// Switch to another project
{
  "action": "change",
  "projectPath": "/workspace/project-b"
}

// Check tasks in the new context
{
  "action": "list"
}
```

### Validating Projects Before Operations

```json
// Validate before switching
{
  "action": "validate",
  "projectPath": "/uncertain/path"
}

// If valid, then switch
{
  "action": "change",
  "projectPath": "/uncertain/path"
}
```

## Benefits

1. **Efficiency**: No need to restart the MCP server when switching projects
2. **Convenience**: Set context once, use everywhere
3. **Flexibility**: Can still override context per tool call when needed
4. **Validation**: Ensures you're working with valid Gradle projects
5. **Transparency**: Always know which project context you're working in
