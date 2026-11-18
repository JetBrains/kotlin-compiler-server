FROM gradle:9-jdk-21-and-24
VOLUME [ "/workspaces" ]
ARG ARCHIVE_FILE
RUN apt-get update && apt-get install -y curl zip unzip tar gzip bash netcat-openbsd \
    && rm -rf /var/lib/apt/lists/*
COPY workspaces /default-workspaces/
WORKDIR /lsp
COPY server/${ARCHIVE_FILE} ./kotlin-server.tar.gz
RUN tar -xzf kotlin-server.tar.gz --strip-components=1 \
    && rm kotlin-server.tar.gz \
    && chmod +x ./bin/intellij-server
EXPOSE 9999
CMD ["./bin/intellij-server", "--multi-client", "--isolated-documents", "--socket", "0.0.0.0:9999"]