FROM openjdk:8-jdk-alpine as build

RUN mkdir -p /kotlin-compiler-server
WORKDIR /kotlin-compiler-server
ADD . /kotlin-compiler-server

RUN ./gradlew build -x test
RUN mkdir -p /build/libs && (cd /build/libs;  jar -xf /kotlin-compiler-server/build/libs/*.jar)

FROM openjdk:8-jdk-alpine

RUN mkdir /kotlin-compiler-server
WORKDIR /kotlin-compiler-server

ENV KOTLIN_LIB=1.4-M1-eap-93
ENV KOTLIN_LIB_JS=1.4-M1-eap-93-js

COPY --from=build /build/libs/BOOT-INF/lib /kotlin-compiler-server/lib
COPY --from=build /build/libs/META-INF /kotlin-compiler-server/META-INF
COPY --from=build /build/libs/BOOT-INF/classes /kotlin-compiler-server
COPY --from=build /kotlin-compiler-server/${KOTLIN_LIB} /kotlin-compiler-server/${KOTLIN_LIB}
COPY --from=build /kotlin-compiler-server/${KOTLIN_LIB_JS} /kotlin-compiler-server/${KOTLIN_LIB_JS}
COPY --from=build /kotlin-compiler-server/executor.policy /kotlin-compiler-server/

ENTRYPOINT ["java", "-noverify", "-cp", "/kotlin-compiler-server:/kotlin-compiler-server/lib/*", "com.compiler.server.CompilerApplicationKt"]

HEALTHCHECK --interval=1m --timeout=3s \
  CMD curl -f http://localhost:8080/health || exit 1
EXPOSE 8080