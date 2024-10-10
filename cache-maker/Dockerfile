FROM amazoncorretto:17

ARG BASE_DIR

RUN mkdir -p $BASE_DIR
WORKDIR $BASE_DIR
ADD . $BASE_DIR

RUN ./gradlew :cache-maker:run
