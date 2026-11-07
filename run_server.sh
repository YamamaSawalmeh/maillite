#!/bin/bash

# Create build directory
mkdir -p build/classes
mkdir -p lib

# Download Gson if missing
if [ ! -f lib/gson-2.10.1.jar ]; then
    echo "Downloading Gson..."
    curl -L -o lib/gson-2.10.1.jar https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar
fi

# Compile
echo "Compiling..."
javac -d build/classes -cp "src/main/java:lib/*" $(find src/main/java -name "*.java")

# Run
echo "Starting Server..."
java -cp "build/classes:lib/gson-2.10.1.jar" com.maillite.server.ServerMain
