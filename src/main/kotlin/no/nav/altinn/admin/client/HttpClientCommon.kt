package no.nav.altinn.admin.client

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.apache.Apache
import io.ktor.client.engine.apache.ApacheEngineConfig
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import java.net.ProxySelector
import no.nav.altinn.admin.common.configuredJacksonMapper
import org.apache.http.impl.conn.SystemDefaultRoutePlanner

fun httpClientDefault() = HttpClient(CIO) {
    install(JsonFeature) {
        serializer = JacksonSerializer(configuredJacksonMapper())
    }
}

val proxyConfig: HttpClientConfig<ApacheEngineConfig>.() -> Unit = {
    install(JsonFeature) {
        serializer = JacksonSerializer(configuredJacksonMapper())
    }
    engine {
        customizeClient {
            setRoutePlanner(SystemDefaultRoutePlanner(ProxySelector.getDefault()))
        }
    }
}

fun httpClientProxy() = HttpClient(Apache, proxyConfig)
