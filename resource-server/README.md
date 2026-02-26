# Resource Server

This is a simple Spring Boot server used for local development to serve static files required by Compose Wasm examples (e.g., Wasm libraries and other static assets).

## Purpose

When running Compose Wasm examples locally, some static files need to be served from a server to avoid CORS issues or to mock a production environment where these files are hosted. This server provides a local endpoint for those resources.

## Configuration

- **Port**: `8081` (configured via `server.port` in `application.properties`).
- **Static Files**: Resources are served from the `static` directory in the classpath. During the build process, these are populated from the `:cache-maker` project and other dependencies.
- **CORS**: The server is configured to allow cross-origin requests from any origin by default for local development convenience.
