#!/usr/bin/env sh

cp -f ../build/distributions/kotlin-compiler-server-*-SNAPSHOT.zip ./kotlin-compiler-server.zip

sam build
sam deploy

aws s3 sync ./build/compose-wasm-resources/ s3://kotlin-compose-stage-composestaticbucket-w4wbe7v2wt1h/api/resource/
aws cloudfront create-invalidation --distribution-id E3LSIHO1W3290M --paths "/*"
