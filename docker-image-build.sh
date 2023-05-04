#!/bin/sh

kotlin_version=$(grep "systemProp.kotlinVersion" gradle.properties | cut -d'=' -f2)

docker build . --file Dockerfile --tag my-image-name:$(date +%s) --build-arg KOTLIN_VERSION=$kotlin_version
