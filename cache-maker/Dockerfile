FROM amazoncorretto:17

ARG BASE_DIR
ARG KOTLIN_VERSION

RUN if [ -z "$KOTLIN_VERSION" ]; then \
        echo "Error: KOTLIN_VERSION argument is not set. Use docker-build-incremental-cache.sh to build the image." >&2; \
        exit 1; \
    fi

ENV KOTLIN_VERSION=$KOTLIN_VERSION

RUN mkdir -p $BASE_DIR
WORKDIR $BASE_DIR
ADD . $BASE_DIR

RUN sed -i 's@kotlin = ".*"@kotlin = "'$KOTLIN_VERSION'"@g' gradle/libs.versions.toml
RUN ./gradlew clean

RUN ./gradlew :cache-maker:compileProductionExecutableKotlinWasmJs
