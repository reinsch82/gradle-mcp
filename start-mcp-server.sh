#!/bin/bash

# Gradle MCP Server Startup Script
# This script builds and starts the Gradle MCP server

set -e

echo "🔨 Building Gradle MCP Server..."
./gradlew build -q

echo "🚀 Starting Gradle MCP Server..."

# Check if running in MCP client mode (STDIO) or standalone HTTP mode
if [ "$1" = "stdio" ]; then
    echo "📡 Starting in STDIO mode for MCP clients..."
    java -jar build/libs/gradle-mcp-1.0-SNAPSHOT.jar
elif [ "$1" = "http" ]; then
    echo "🌐 Starting HTTP server on port 8080..."
    echo "📍 MCP SSE endpoint: http://localhost:8080/mcp/sse"
    echo "📊 Health endpoint: http://localhost:8080/actuator/health"
    java -jar build/libs/gradle-mcp-1.0-SNAPSHOT.jar --server.port=8080
else
    echo "Usage: $0 [stdio|http]"
    echo ""
    echo "  stdio  - Start in STDIO mode for MCP clients (Claude Desktop, VS Code, etc.)"
    echo "  http   - Start HTTP server with SSE transport for web clients"
    echo ""
    echo "Examples:"
    echo "  $0 stdio   # For use with Claude Desktop"
    echo "  $0 http    # For HTTP-based MCP clients"
    exit 1
fi
