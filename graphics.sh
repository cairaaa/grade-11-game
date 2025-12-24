#!/bin/bash

if [ $# -ne 1 ]; then
    echo "Usage: $0 (file name)"
    exit 1
fi

FILENAME=$1
if [ ! -f "$FILENAME" ]; then
    echo "Error: File '$FILENAME' not found."
    exit 1
fi

LINUX_DIR=$(pwd)
WINDOWS_DIR=$(wslpath -w "$LINUX_DIR")

WIN_JAVAC="/mnt/c/Program Files/Java/jdk-24/bin/javac.exe"
WIN_JAVA="/mnt/c/Program Files/Java/jdk-24/bin/java.exe"

if [ ! -f "$WIN_JAVAC" ] || [ ! -f "$WIN_JAVA" ]; then
    WIN_JAVAC="/mnt/c/Program Files/Java/jdk-25/bin/javac.exe"
    WIN_JAVA="/mnt/c/Program Files/Java/jdk-25/bin/java.exe"
fi

"$WIN_JAVAC" "$FILENAME" || exit 1

CLASSNAME="${FILENAME%.java}"

"$WIN_JAVA" -cp "$WINDOWS_DIR" "$CLASSNAME" &
JAVA_PID=$!

read
kill $JAVA_PID 2>/dev/null
taskkill.exe /F /IM java.exe 2>/dev/null