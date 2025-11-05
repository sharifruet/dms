#!/bin/bash

# Script to run the backend locally using Maven wrapper
# Make sure Docker services (postgres, redis, elasticsearch) are running first

echo "🚀 Starting DMS Backend with Maven Wrapper..."
echo ""

# Check if Docker services are running
echo "Checking Docker services..."
if ! docker ps | grep -q "dms-postgres\|dms-redis\|dms-elasticsearch"; then
    echo "⚠️  Warning: Docker services may not be running."
    echo "   Start them with: docker compose -f ../docker-compose.yml up -d postgres redis elasticsearch"
    echo "   Waiting 5 seconds for you to start them..."
    sleep 5
    echo ""
fi

# Check Java version
echo "Checking Java version..."
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -gt "21" ]; then
    echo "⚠️  Warning: Java $JAVA_VERSION detected. Project requires Java 21."
    echo "   Consider using Java 21 for compatibility."
    echo ""
fi

# Run with local profile
echo "Starting Spring Boot application with 'local' profile..."
echo "Press Ctrl+C to stop"
echo ""

# macOS/Homebrew: ensure Tesseract native libs and tessdata are visible to JVM
if [ -d "/opt/homebrew/share/tessdata" ]; then
    export TESSDATA_PREFIX="/opt/homebrew/share/tessdata"
elif [ -d "/usr/local/share/tessdata" ]; then
    export TESSDATA_PREFIX="/usr/local/share/tessdata"
fi

# Library paths for tess4j to load libtesseract and dependencies
BREW_LIB=""
if [ -d "/opt/homebrew/lib" ]; then
    BREW_LIB="/opt/homebrew/lib"
elif [ -d "/usr/local/lib" ]; then
    BREW_LIB="/usr/local/lib"
fi

if [ -n "$BREW_LIB" ]; then
    export DYLD_LIBRARY_PATH="$BREW_LIB:${DYLD_LIBRARY_PATH}"
    export JNA_LIBRARY_PATH="$BREW_LIB:${JNA_LIBRARY_PATH}"
    JVM_LIB_OPTS="-Djava.library.path=$BREW_LIB -Djna.library.path=$BREW_LIB"
else
    JVM_LIB_OPTS=""
fi

# Ensure Homebrew bin is on PATH so 'tesseract' CLI is found for fallback
if [ -d "/opt/homebrew/bin" ]; then
    export PATH="/opt/homebrew/bin:$PATH"
elif [ -d "/usr/local/bin" ]; then
    export PATH="/usr/local/bin:$ PATH"
fi

# Capture errors and show them
JNA_DEBUG_OPTS="-Djna.debug_load=true -Djna.debug_load.jna=true"
./mvnw spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.jvmArguments="$JVM_LIB_OPTS $JNA_DEBUG_OPTS" 2>&1 | tee /tmp/dms-backend.log

