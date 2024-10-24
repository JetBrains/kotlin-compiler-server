FROM amazoncorretto:17

RUN mkdir -p /var/task
WORKDIR /var/task
ADD . /var/task

RUN ./gradlew :cache-maker:run
