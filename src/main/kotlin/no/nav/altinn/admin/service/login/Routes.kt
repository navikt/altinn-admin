package no.nav.altinn.admin.service.login

import io.ktor.application.call
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.locations.Location
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import mu.KotlinLogging
import no.nav.altinn.admin.Environment
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.Group
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.badRequest
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.get
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.ok
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.responds
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.serviceUnavailable
import no.nav.altinn.admin.client.azuread.AzureAdClient
import no.nav.altinn.admin.client.wellknown.getWellKnown
import no.nav.altinn.admin.common.API_V1

fun Routing.loginAPI(environment: Environment, httpClient: HttpClient) {
    // getToken(environment)
    // getToken2(environment)
    getLogin(environment, httpClient)
}

internal data class AnError(val error: String)
internal const val GROUP_NAME = "Login"

private val logger = KotlinLogging.logger { }

@Group(GROUP_NAME)
@Location("$API_V1/login")
class Test

fun Routing.getLogin(environment: Environment, httpClient: HttpClient) =
    get<Test> (
        "Login".responds(
            ok<String>(), serviceUnavailable<AnError>(), badRequest<AnError>()
        )
    ) {
        httpClient.get<Any>("https://altinn-admin.dev.intern.nav.no/oauth2/login")
        val userSession: UserSession? = call.sessions.get<UserSession>()
        if (userSession != null) {
            logger.info { "Got usersession here" }
        } else {
            logger.info { "usersession is null, too soon" }
        }
        call.respond(HttpStatusCode.OK, "Token: MAYBE HERE...")
    }

@Group(GROUP_NAME)
@Location("$API_V1/test/wellknown/")
class GetToken

fun Routing.getToken(environment: Environment) =
    get<GetToken> (
        "WK test".responds(
            ok<Any>(), serviceUnavailable<AnError>(), badRequest<AnError>()
        )
    ) {
        logger.info { "Testing wellknown" }
        val wellKnownInternalAzureAd = getWellKnown(
            wellKnownUrl = environment.azure.azureAppWellKnownUrl
        )
        logger.info { "issuer: ${wellKnownInternalAzureAd.issuer}" }
        if (wellKnownInternalAzureAd.issuer.isNotEmpty())
            call.respond(HttpStatusCode.OK, "OK")
        else
            call.respond(HttpStatusCode.InternalServerError, "No issuer found")
    }

@Group(GROUP_NAME)
@Location("$API_V1/test/aad/")
class GetToken2
fun Routing.getToken2(environment: Environment) =
    get<GetToken2> (
        "AAD test".responds(
            ok<Any>(), serviceUnavailable<AnError>(), badRequest<AnError>()
        )
    ) {
        logger.info { "Testing aad" }
        val aadClient = AzureAdClient(
            environment.azure.azureAppClientId,
            environment.azure.azureAppClientSecret,
            environment.azure.azureOpenidConfigTokenEndpoint
        )
//        val resp = aadClient.getOnBehalfOfToken()
//        logger.info { "issuer: ${wellKnownInternalAzureAd.issuer}" }
//        if (wellKnownInternalAzureAd.issuer.isNotEmpty())
        call.respond(HttpStatusCode.OK, "OK")
//        else
//            call.respond(HttpStatusCode.InternalServerError, "No issuer found")
    }
