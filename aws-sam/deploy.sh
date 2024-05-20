#!/usr/bin/env sh

cp -f ../build/distributions/kotlin-compiler-server-*-SNAPSHOT.zip ./kotlin-compiler-server.zip

sam build
sam deploy