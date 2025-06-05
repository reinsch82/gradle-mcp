#!/usr/bin/env python3
"""
Simple MCP server validation script for testing the Gradle MCP server
"""
import asyncio
import json
import subprocess
import sys
from typing import Dict, Any

class MCPTester:
    def __init__(self, command: list[str]):
        self.command = command
        self.process = None
        
    async def send_request(self, method: str, params: Dict[str, Any] = None) -> Dict[str, Any]:
        """Send a JSON-RPC request to the MCP server"""
        request = {
            "jsonrpc": "2.0",
            "id": 1,
            "method": method
        }
        if params:
            request["params"] = params
            
        request_json = json.dumps(request) + "\n"
        
        print(f"🔍 Testing {method}...")
        print(f"📤 Sending: {request_json.strip()}")
        
        # Send request
        self.process.stdin.write(request_json.encode())
        await self.process.stdin.drain()
        
        # Read response
        response_line = await self.process.stdout.readline()
        response_text = response_line.decode().strip()
        
        print(f"📥 Received: {response_text}")
        
        try:
            response = json.loads(response_text)
            return response
        except json.JSONDecodeError as e:
            print(f"❌ Failed to parse JSON response: {e}")
            return {"error": "Invalid JSON response"}
    
    async def start_server(self):
        """Start the MCP server process"""
        print(f"🚀 Starting MCP server: {' '.join(self.command)}")
        self.process = await asyncio.create_subprocess_exec(
            *self.command,
            stdin=asyncio.subprocess.PIPE,
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.PIPE
        )
        
    async def stop_server(self):
        """Stop the MCP server process"""
        if self.process:
            self.process.terminate()
            await self.process.wait()
            
    async def test_basic_functionality(self):
        """Test basic MCP functionality"""
        print("=" * 60)
        print("🧪 Testing Gradle MCP Server")
        print("=" * 60)
        
        try:
            # Start server
            await self.start_server()
            
            # Wait a moment for server to start
            await asyncio.sleep(1)
            
            # Test initialization
            init_response = await self.send_request("initialize", {
                "protocolVersion": "2024-11-05",
                "capabilities": {},
                "clientInfo": {
                    "name": "test-client",
                    "version": "1.0.0"
                }
            })
            
            if "result" in init_response:
                print("✅ Server initialization successful")
                server_info = init_response["result"].get("serverInfo", {})
                print(f"   Server: {server_info.get('name', 'Unknown')} v{server_info.get('version', 'Unknown')}")
            else:
                print(f"❌ Server initialization failed: {init_response}")
                return False
            
            # Test tools list
            tools_response = await self.send_request("tools/list")
            
            if "result" in tools_response:
                tools = tools_response["result"].get("tools", [])
                print(f"✅ Found {len(tools)} tools:")
                for tool in tools:
                    print(f"   - {tool.get('name', 'Unknown')}: {tool.get('description', 'No description')}")
            else:
                print(f"❌ Failed to list tools: {tools_response}")
                return False
                
            # Test project context tool specifically
            if any(tool.get('name') == 'gradle_project_context' for tool in tools):
                print("\n🎯 Testing gradle_project_context tool...")
                context_response = await self.send_request("tools/call", {
                    "name": "gradle_project_context",
                    "arguments": {
                        "action": "get"
                    }
                })
                
                if "result" in context_response:
                    print("✅ gradle_project_context tool works correctly")
                    content = context_response["result"].get("content", [])
                    if content:
                        print(f"   Response: {content[0].get('text', 'No text')[:100]}...")
                else:
                    print(f"❌ gradle_project_context tool failed: {context_response}")
            
            print("\n🎉 All tests completed!")
            return True
            
        except Exception as e:
            print(f"❌ Test failed with exception: {e}")
            return False
        finally:
            await self.stop_server()

async def main():
    if len(sys.argv) < 2:
        print("Usage: python test_mcp_server.py <jar_file_path>")
        print("Example: python test_mcp_server.py build/libs/gradle-mcp-1.0-SNAPSHOT.jar")
        sys.exit(1)
    
    jar_path = sys.argv[1]
    command = ["java", "-jar", jar_path]
    
    tester = MCPTester(command)
    success = await tester.test_basic_functionality()
    
    if success:
        print("\n✅ MCP server validation passed!")
        sys.exit(0)
    else:
        print("\n❌ MCP server validation failed!")
        sys.exit(1)

if __name__ == "__main__":
    asyncio.run(main())
