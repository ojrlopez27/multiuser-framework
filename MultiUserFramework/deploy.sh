#!/bin/sh


echo "uploading the whole stuff..."
gradle clean --refresh-dependencies
gradle build -x test
gradle uploadShadow
cd ..
