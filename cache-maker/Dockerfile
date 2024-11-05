FROM amazoncorretto:17

ARG BASE_DIR
ARG KOTLIN_VERSION

RUN if [ -z "$KOTLIN_VERSION" ]; then \
        echo "Error: KOTLIN_VERSION argument is not set. Use docker-build-incremental-cache.sh to build the image." >&2; \
        exit 1; \
    fi

ENV KOTLIN_LIB=$KOTLIN_VERSION
ENV KOTLIN_LIB_JS=${KOTLIN_VERSION}-js
ENV KOTLIN_LIB_WASM=${KOTLIN_VERSION}-wasm
ENV KOTLIN_LIB_COMPOSE_WASM=${KOTLIN_VERSION}-compose-wasm
ENV KOTLIN_COMPOSE_WASM_COMPILER_PLUGINS=${KOTLIN_VERSION}-compose-wasm-compiler-plugins
ENV KOTLIN_CACHES_COMPOSE_WASM=${KOTLIN_VERSION}-caches-compose-wasm

RUN mkdir -p $BASE_DIR
WORKDIR $BASE_DIR
ADD . $BASE_DIR

RUN rm -rf $KOTLIN_LIB
RUN rm -rf $KOTLIN_LIB_JS
RUN rm -rf $KOTLIN_LIB_WASM
RUN rm -rf $KOTLIN_LIB_COMPOSE_WASM
RUN rm -rf $KOTLIN_COMPOSE_WASM_COMPILER_PLUGINS
RUN rm -rf $KOTLIN_CACHES_COMPOSE_WASM
RUN ./gradlew clean

RUN ./gradlew :cache-maker:run
