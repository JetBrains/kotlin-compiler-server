#!/bin/sh

kotlinVersion=$(awk '{ if ($1 == "kotlin") { gsub(/"/, "", $2); print $2; } }' FS=' = ' ./gradle/libs.versions.toml)

echo "Kotlin Version for the docker: $kotlinVersion"

docker build . --file Dockerfile --tag my-image-name:$(date +%s) --build-arg KOTLIN_VERSION=$kotlinVersion --build-arg DEVELOCITY_ACCESS_KEY=$DEVELOCITY_ACCESS_KEY
