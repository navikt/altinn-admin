package no.nav.altinn.admin.client.azuread

import io.ktor.client.call.receive
import io.ktor.client.features.ClientRequestException
import io.ktor.client.features.ResponseException
import io.ktor.client.features.ServerResponseException
import io.ktor.client.request.accept
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import mu.KotlinLogging
import no.nav.altinn.admin.client.httpClientProxy

private val logger = KotlinLogging.logger { }

class AzureAdClient(
    private val azureAppClientId: String,
    private val azureAppClientSecret: String,
    private val azureOpenidConfigTokenEndpoint: String,
    private val graphApiUrl: String,
) {
    private val httpClient = httpClientProxy()

    suspend fun getOnBehalfOfToken(
        scopeClientId: String,
        token: String,
    ): AzureAdToken? {
        val scope = if (scopeClientId == graphApiUrl) {
            "$scopeClientId/.default"
        } else {
            "api://$scopeClientId/.default"
        }
        return getAccessToken(
            Parameters.build {
                append("client_id", azureAppClientId)
                append("client_secret", azureAppClientSecret)
                append("client_assertion_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                append("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                append("assertion", token)
                append("scope", scope)
                append("requested_token_use", "on_behalf_of")
            }
        )?.toAzureAdToken()
    }

    private suspend fun getAccessToken(
        formParameters: Parameters,
    ): AzureAdTokenResponse? {
        return try {
            val response: HttpResponse = httpClient.post(azureOpenidConfigTokenEndpoint) {
                accept(ContentType.Application.Json)
                body = FormDataContent(formParameters)
            }
            response.receive<AzureAdTokenResponse>()
        } catch (e: ClientRequestException) {
            handleUnexpectedResponseException(e)
            null
        } catch (e: ServerResponseException) {
            handleUnexpectedResponseException(e)
            null
        }
    }

    private fun handleUnexpectedResponseException(
        responseException: ResponseException,
    ) {
        logger.error(
            "Error while requesting AzureAdAccessToken with statusCode=${responseException.response.status.value}",
            responseException
        )
    }
}
