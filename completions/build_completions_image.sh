#!/bin/bash

export COMPLETIONS_SERVICE_PORT=8081

# export LSP_LOCAL_WORKSPACE_ROOT=lsp-users-projects-root
export LSP_REMOTE_WORKSPACE_ROOT=lsp-users-projects-root-test

echo "building docker image for spring application"
yes n | ../gradlew bootBuildImage

if [[ " $* " == *" --run "* ]]; then
  echo "requested starting completion service"
  docker compose up -d
  echo "completion service up and running"
fi

exit 0
