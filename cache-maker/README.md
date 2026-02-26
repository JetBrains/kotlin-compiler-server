# Cache Maker

This module is responsible for preparing pre-compiled static files (Compose Wasm libraries) that are used by the Kotlin Compiler Server and the Resource Server.

## Purpose

When running Compose Wasm examples, the browser needs access to pre-compiled Wasm and JavaScript files for the Compose runtime and other libraries. `cache-maker` automates the process of:
1.  Compiling Kotlin/Wasm (Klib) libraries into Wasm and MJS files.
2.  Optimizing the Wasm output using Binaryen.
3.  Versioning files using content hashes to avoid caching issues.
4.  Bundling resources into a format ready to be served by a static server.

## Key Build Tasks

- **`prepareRuntime`**: Collects compiled Wasm and MJS files from dependencies and NPM modules (like `@js-joda`).
- **`calculateHash`**: Generates a content hash based on the prepared runtime files.
- **`prepareComposeWasmResources`**: Renames files to include the content hash, updates internal imports in MJS files, and injects buffering logic into the Kotlin standard library for output capturing.
- **`bundledRuntimes`**: Further processes the resources, merging Wasm outputs into JS files where applicable (excluding Skiko).

## Consumption

The outputs of this module are consumed by:
- **`:resource-server`**: Uses the prepared resources to serve them during local development.
- **Root Project**: Packages these resources into the production build (e.g., for Lambda deployment).

The module exposes two main Gradle configurations for other projects:
- `kotlinComposeWasmRuntime`: The directory containing the processed static resources.
- `kotlinComposeWasmRuntimeHash`: A file containing the calculated content hash.

## Local Development

Usually, you don't need to run tasks in this module directly. Running `Resource server` or building the main project will automatically trigger the necessary tasks in `cache-maker`.
