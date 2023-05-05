#!/bin/sh

kotlin_version=$(awk '{ if ($1=="systemProp.kotlinVersion") { print $2; } }' FS='=' ./gradle.properties)

docker build . --file Dockerfile --tag my-image-name:$(date +%s) --build-arg KOTLIN_VERSION=$kotlin_version
