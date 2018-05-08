#!/bin/sh

echo "uploading commons..."
cd commons
gradle clean build -x test
gradle uploadShadow
cd ..

echo "uploading client..."
cd client
gradle clean build -x test
gradle uploadShadow
cd ..

echo "uploading server - whole MUF..."
gradle clean --refresh-dependencies
gradle build -x test
gradle uploadShadow



