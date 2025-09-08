FROM gradle:9-jdk-21-and-24

RUN apt-get update && apt-get install -y curl zip unzip bash netcat-openbsd \
    && rm -rf /var/lib/apt/lists/*

COPY lsp-users-projects-root-test/ /lsp-users-projects-root-test/
WORKDIR /lsp
ARG LSP_ZIP=intellij-lsp-253.27396.0-linux-x64.zip
COPY server/${LSP_ZIP} .
RUN unzip ${LSP_ZIP} \
    && rm ${LSP_ZIP} \
    && chmod +x ./kotlin-lsp.sh

EXPOSE 9999
CMD ["./kotlin-lsp.sh", "--multi-client", "--isolated-documents","--socket", "0.0.0.0:9999"]
