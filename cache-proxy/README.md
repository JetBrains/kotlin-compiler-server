# Cache Proxy

AWS Lambda that sits in front of the main compiler Lambda and caches successful
Compose Wasm translation results in Valkey (Redis-compatible).

## Purpose

Compose Wasm translation is expensive and deterministic for identical inputs.
The proxy receives the API Gateway event, checks Valkey for a cached response,
and either:
- returns the cache hit directly, or
- forwards the event to the target compiler Lambda, then stores the successful,
  error-free response in Valkey before returning it.

Only `/translate` requests with `compiler=compose-wasm` (V1) and requests to
`/translate/compose-wasm` (V2) are cacheable; everything else is forwarded
unchanged.

## Architecture

Unlike the main module (Spring Boot running in a container), `cache-proxy` is a
plain AWS Lambda using the low-level `RequestStreamHandler` API — no web
framework, no DI container. Keeping the handler lean keeps cold-start time
low.

Key dependencies:
- `software.amazon.awssdk:lambda` — invokes the target (compiler) Lambda.
- `io.lettuce:lettuce-core` — Valkey/Redis client. A single synchronous
  connection is created per Lambda container and reused across invocations.
- `jackson-module-kotlin` — request/response parsing and canonical JSON
  serialization for cache-key hashing.

The cache key is `SHA-256` over a canonical (alphabetically sorted) JSON
representation of the normalized request, prefixed with version (`v1`/`V2`),
namespace, and Kotlin version.

## Configuration

Configured entirely via environment variables:

| Variable             | Purpose                                              |
|----------------------|------------------------------------------------------|
| `TARGET_LAMBDA_NAME` | Name of the compiler Lambda to forward requests to. |
| `KOTLIN_VERSION`     | Included in the cache key so different compiler versions don't collide. |
| `CACHE_NAMESPACE`    | Optional cache-key prefix (default: `default`).     |
| `REDIS_HOST`         | Valkey endpoint hostname.                           |
| `REDIS_PORT`         | Valkey port (default: `6379`).                      |
| `REDIS_SSL`          | `true` to enable TLS (default: `false`).            |

Cached entries have a 24-hour TTL, refreshed on every hit.

## Build

The module produces a Lambda deployment zip:

```
./gradlew :cache-proxy:buildProxyLambda
```

Output: `cache-proxy/build/distributions/cache-proxy.zip`.

## Local development

The handler is designed for the Lambda runtime, so there is no local HTTP
server. Iterate via unit tests (`./gradlew :cache-proxy:test`) or deploy the
zip to a dev Lambda and invoke it through API Gateway.
