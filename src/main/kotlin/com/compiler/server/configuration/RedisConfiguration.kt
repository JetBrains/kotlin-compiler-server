package com.compiler.server.configuration

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest

@Configuration
@ConditionalOnProperty("wasm.compose.cache.enabled")
class RedisConfiguration(
    @Value("\${spring.data.redis.host}") private val host: String,
    @Value("\${spring.data.redis.port:6379}") private val port: Int,
    @Value("\${spring.data.redis.ssl.enabled:false}") private val sslEnabled: Boolean,
    @Value("\${valkey.secret.arn:}") private val secretArn: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    fun lettuceConnectionFactory(): LettuceConnectionFactory {
        val config = RedisStandaloneConfiguration(host, port)

        if (secretArn.isNotBlank()) {
            log.info("Loading Valkey auth token from Secrets Manager")
            config.setPassword(loadPasswordFromSecretsManager(secretArn))
        }

        val clientConfig = LettuceClientConfiguration.builder()
        if (sslEnabled) {
            clientConfig.useSsl()
        }

        return LettuceConnectionFactory(config, clientConfig.build())
    }

    private fun loadPasswordFromSecretsManager(arn: String): String {
        return SecretsManagerClient.create().use { client ->
            client.getSecretValue(
                GetSecretValueRequest.builder().secretId(arn).build()
            ).secretString()
        }
    }
}
