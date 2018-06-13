#!/bin/sh

#echo "uploading commons..."
#cd commons
#gradle clean build -x test
#gradle uploadShadow
#cd ..

#echo "uploading core..."
#cd core
#gradle clean build -x test
#gradle uploadShadow
#cd ..

#echo "uploading client..."
#cd client
#gradle clean build -x test
#gradle uploadShadow
#cd ..

#echo "uploading server..."
#cd server
#gradle clean --refresh-dependencies
#gradle build -x test
#gradle uploadShadow
#cd ..

#echo "uploading composition..."
#cd composition
#gradle clean --refresh-dependencies
#gradle build -x test
#gradle uploadShadow
#cd ..

echo "uploading the whole stuff..."
gradle clean --refresh-dependencies
gradle build -x test
gradle uploadShadow
cd ..
