#!/bin/bash
# Test script for resolve_hotswap_url function

# Source the function from the main script
source .devcontainer/setup-jdk.sh

# Test function directly
echo "Testing resolve_hotswap_url function..."
echo "=========================="

echo "Test 1: latest version"
url=$(resolve_hotswap_url "latest")
echo "Result: '$url'"
echo

echo "Test 2: specific version"
url=$(resolve_hotswap_url "2.0.1") 
echo "Result: '$url'"
echo

echo "Test 3: Check no debug output in result"
result=$(resolve_hotswap_url "latest")
if echo "$result" | grep -q "Querying GitHub"; then
    echo "ERROR: Debug output leaked into result!"
    echo "Result was: '$result'"
else
    echo "SUCCESS: Clean URL result"
fi
