package no.nav.altinn.admin.client

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.logging.DEFAULT
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logger
import io.ktor.client.features.logging.Logging
import io.ktor.client.request.forms.submitForm
import io.ktor.http.parametersOf
import mu.KotlinLogging
import no.nav.altinn.admin.Environment
import no.nav.altinn.admin.common.objectMapper

private val logger = KotlinLogging.logger { }

class MaskinportenClient(val environment: Environment) {

    private val clientAuthentication = ClientAuthentication(environment.maskinporten)

    suspend fun tokenRequest() = tokenString(
        environment.maskinporten.baseUrl + "token",
        clientAuthentication.clientAssertion()
    )

    internal val defaultHttpClient = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = JacksonSerializer { objectMapper }
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.NONE
        }
    }

    suspend fun tokenString(url: String, assertion: String): String {
        logger.info { "Request Accesstoken $url" }
        return defaultHttpClient.submitForm<AccessTokenResponse>(
            url,
            formParameters = parametersOf(
                PARAMS_GRANT_TYPE to listOf(GRANT_TYPE_JWT_BEARER),
                PARAMS_ASSERTION to listOf(assertion)
            )
        ).accessToken
    }

    companion object {
        internal const val PARAMS_GRANT_TYPE = "grant_type"
        internal const val GRANT_TYPE_JWT_BEARER = "urn:ietf:params:oauth:grant-type:jwt-bearer"
        internal const val PARAMS_ASSERTION = "assertion"
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class AccessTokenResponse(
    @JsonProperty(value = "access_token", required = true) val accessToken: String,
    @JsonProperty(value = "token_type", required = true) val tokenType: String,
    @JsonProperty(value = "expires_in", required = true) val expiresIn: Int
)
