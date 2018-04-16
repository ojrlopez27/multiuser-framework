#!/bin/sh
echo "uploading commons..."
cd commons
gradle clean build
gradle uploadShadow
cd ..

echo "uploading client..."
cd client
gradle clean build
gradle uploadShadow
cd ..

echo "uploadin server - whole MUF..."
gradle clean --refresh-dependencies
gradle build -x test
gradle uploadShadow



