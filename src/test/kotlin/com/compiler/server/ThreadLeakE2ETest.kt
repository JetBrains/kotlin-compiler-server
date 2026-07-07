package com.compiler.server

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.images.builder.ImageFromDockerfile
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration

/**
 * KTL-4631 — e2e reproduction of the `JavaExecutor` native-thread leak.
 * Hammers `/api/compiler/run` in a pid-capped container: buggy code leaks 2 threads/run,
 * crosses [PIDS_LIMIT] and fails; after the `shutdown()` fix all runs stay clean.
 * Heavy + slow. Excluded from the default `test` task; run with `./gradlew leakTest`.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ThreadLeakE2ETest {

    companion object {
        private const val PORT = 8080

        /**
         * cgroup pid cap (threads count as pids). Must sit above the per-request transient peak
         * (server + in-process compile threads + the forked child JVM ~ up to ~200) so a healthy
         * run never trips it, yet low enough that the 2-threads/run leak crosses it quickly.
         * Empirically: 512 → buggy crosses @228; 256 → clean; 160 → false positive @50.
         */
        private const val PIDS_LIMIT = 256L

        /** Upper bound on requests. Leak crosses the pid cap well before this; a clean run means no leak. */
        private const val MAX_ITERATIONS = 150

        private const val REQUEST_TIMEOUT_SECONDS = 30L

        private val NATIVE_THREAD_MARKERS = listOf(
            "unable to create native thread",
            "Resource temporarily unavailable",
            "error=11",
        )
    }

    private val kotlinVersion: String =
        System.getProperty("e2e.kotlin.version")
            ?: error("System property 'e2e.kotlin.version' is not set (configured by the 'leakTest' Gradle task).")

    private val jarPath: Path =
        Paths.get("build/libs/kotlin-compiler-server-$kotlinVersion-SNAPSHOT.jar").toAbsolutePath()

    private val dockerfile: String = buildString {
        appendLine("FROM amazoncorretto:17-al2023")
        appendLine("WORKDIR /app")
        appendLine("COPY app.jar /app/app.jar")
        appendLine("COPY $kotlinVersion /app/$kotlinVersion")
        appendLine("""CMD ["java", "-Dserver.port=$PORT", "-jar", "/app/app.jar"]""")
    }

    private val image: ImageFromDockerfile = ImageFromDockerfile()
        .withFileFromString("Dockerfile", dockerfile)
        .withFileFromPath("app.jar", jarPath)
        .withFileFromPath(kotlinVersion, Paths.get(kotlinVersion).toAbsolutePath())

    private val container: GenericContainer<*> = GenericContainer(image)
        .withExposedPorts(PORT)
        .withCreateContainerCmdModifier { cmd ->
            cmd.hostConfig
                ?.withPidsLimit(PIDS_LIMIT)
                ?.withMemory(4L * 1024 * 1024 * 1024) // 4 GiB: comfortable heap so the leak surfaces as pid exhaustion, not heap OOM
        }
        .waitingFor(
            Wait.forHttp("/health")
                .forPort(PORT)
                .forStatusCode(200)
                .withStartupTimeout(Duration.ofMinutes(6))
        )

    @BeforeAll
    fun startContainer() = container.start()

    @Test
    fun `run endpoint does not leak native threads under repeated calls`() {
        val client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
            .build()

        val baseUrl = "http://${container.host}:${container.getMappedPort(PORT)}"
        val body = jacksonObjectMapper().writeValueAsString(
            mapOf(
                "args" to "",
                "files" to listOf(mapOf("name" to "File.kt", "text" to "fun main() { println(\"ok\") }")),
                "compilerArguments" to emptyMap<String, Any>(),
            )
        )

        repeat(MAX_ITERATIONS) { iteration ->
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/api/compiler/run"))
                .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build()

            val response: HttpResponse<String> = try {
                client.send(request, HttpResponse.BodyHandlers.ofString())
            } catch (e: Exception) {
                // Server can no longer service the request (thread starvation on the web tier).
                fail<Nothing>(
                    "Native-thread leak reproduced at iteration $iteration/$MAX_ITERATIONS: " +
                        "request failed with ${e.javaClass.simpleName}: ${e.message}"
                )
            }

            val payload = response.body() ?: ""
            val marker = NATIVE_THREAD_MARKERS.firstOrNull { payload.contains(it, ignoreCase = true) }

            if (response.statusCode() >= 500 || marker != null) {
                fail<Nothing>(
                    """
                    Native-thread leak reproduced at iteration $iteration/$MAX_ITERATIONS.
                    HTTP status : ${response.statusCode()}
                    Marker      : ${marker ?: "HTTP ${response.statusCode()}"}
                    Body        : ${payload.take(2000)}
                    """.trimIndent()
                )
            }
        }
    }
}
