FROM gradle:9-jdk-21-and-24

ARG ZIP_VERSION
ARG ZIP_ARCH
ENV ZIP_VERSION=${ZIP_VERSION}
ENV ZIP_ARCH=${ZIP_ARCH}

RUN apt-get update && apt-get install -y curl zip unzip bash netcat-openbsd \
    && rm -rf /var/lib/apt/lists/*

COPY lsp-users-projects-root-test/ /lsp-users-projects-root-test/
WORKDIR /lsp

COPY server/intellij-lsp-${ZIP_VERSION}-linux-${ZIP_ARCH}.zip ./intellij-lsp.zip

RUN unzip intellij-lsp.zip \
    && rm intellij-lsp.zip \
    && chmod +x ./kotlin-lsp.sh

EXPOSE 9999

CMD ["./kotlin-lsp.sh", "--multi-client", "--isolated-documents", "--socket", "0.0.0.0:9999"]
