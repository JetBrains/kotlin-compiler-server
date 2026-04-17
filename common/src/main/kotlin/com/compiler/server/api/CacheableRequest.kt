package com.compiler.server.api

/**
 * Marker interface implemented by request types whose compilation result
 * can be cached by the cache-proxy Lambda.
 *
 * Used as the common parent in [com.compiler.server.cacheproxy.service.CacheService]
 * so cache-key building can pattern-match exhaustively over known request types.
 */
interface CacheableRequest
