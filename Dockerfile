FROM amazoncorretto:17 as build

ARG KOTLIN_VERSION

RUN if [ -z "$KOTLIN_VERSION" ]; then \
        echo "Error: KOTLIN_VERSION argument is not set. Use docker-image-build.sh to build the image." >&2; \
        exit 1; \
    fi

ENV KOTLIN_LIB=$KOTLIN_VERSION
ENV KOTLIN_LIB_JS=${KOTLIN_VERSION}-js

RUN mkdir -p /kotlin-compiler-server
WORKDIR /kotlin-compiler-server
ADD . /kotlin-compiler-server

RUN ./gradlew build -x test
RUN mkdir -p /build/libs && (cd /build/libs;  jar -xf /kotlin-compiler-server/build/libs/kotlin-compiler-server-${KOTLIN_LIB}-SNAPSHOT.jar)

FROM amazoncorretto:17

RUN mkdir /kotlin-compiler-server
WORKDIR /kotlin-compiler-server

COPY --from=build /build/libs/BOOT-INF/lib /kotlin-compiler-server/lib
COPY --from=build /build/libs/META-INF /kotlin-compiler-server/META-INF
COPY --from=build /build/libs/BOOT-INF/classes /kotlin-compiler-server
COPY --from=build /kotlin-compiler-server/${KOTLIN_LIB} /kotlin-compiler-server/${KOTLIN_LIB}
COPY --from=build /kotlin-compiler-server/${KOTLIN_LIB_JS} /kotlin-compiler-server/${KOTLIN_LIB_JS}
COPY --from=build /kotlin-compiler-server/executor.policy /kotlin-compiler-server/
COPY --from=build /kotlin-compiler-server/indexes.json /kotlin-compiler-server/

ENV PORT=8080

CMD ["java", "-noverify", \
    "-Dserver.port=${PORT}", \
    "-cp", "/kotlin-compiler-server:/kotlin-compiler-server/lib/*", \
    "com.compiler.server.CompilerApplicationKt"]
