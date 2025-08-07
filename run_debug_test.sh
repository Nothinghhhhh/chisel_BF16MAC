#!/bin/bash

echo "Running MulAddRecFN Debug Test..."

# 运行简单的debug测试
sbt "testOnly macunit.SimpleDebugTest"

echo "Test completed!" 