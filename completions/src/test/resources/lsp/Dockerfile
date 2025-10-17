FROM gradle:9-jdk-21-and-24

RUN apt-get update && apt-get install -y curl zip unzip bash netcat-openbsd \
    && rm -rf /var/lib/apt/lists/*

COPY lsp-users-projects-root-test/ /lsp-users-projects-root-test/
WORKDIR /lsp

COPY server/*-linux-x64.zip .

RUN unzip *-linux-x64.zip \
    && rm *-linux-x64.zip \
    && chmod +x ./kotlin-lsp.sh

EXPOSE 9999
CMD ["./kotlin-lsp.sh", "--multi-client", "--isolated-documents", "--socket", "0.0.0.0:9999"]
