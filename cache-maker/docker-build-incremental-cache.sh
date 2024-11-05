#!/bin/sh

kotlinVersion=$(awk '{ if ($1 == "kotlin") { gsub(/"/, "", $2); print $2; } }' FS=' = ' ./gradle/libs.versions.toml)

baseDir=$1
targetDir=$2

echo "Kotlin Version for the docker: $kotlinVersion"
echo "Base directory: $baseDir"
echo "Target directory: $targetDir"

image_tag=my-image-name:$(date +%s)

docker build . --file cache-maker/Dockerfile --tag $image_tag --build-arg BASE_DIR=$baseDir --build-arg KOTLIN_VERSION=$kotlinVersion

container=$(docker create $image_tag)

docker cp $container:$baseDir/$kotlinVersion-caches-compose-wasm $targetDir

docker start $container
docker stop $container
docker remove $container