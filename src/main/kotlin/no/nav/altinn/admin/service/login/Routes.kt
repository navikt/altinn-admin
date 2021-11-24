package no.nav.altinn.admin.service.login

import io.ktor.application.call
import io.ktor.auth.OAuthAccessTokenResponse
import io.ktor.auth.authenticate
import io.ktor.auth.principal
import io.ktor.http.HttpStatusCode
import io.ktor.locations.Location
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.sessions.sessions
import io.ktor.sessions.set
import mu.KotlinLogging
import no.nav.altinn.admin.Environment
import no.nav.altinn.admin.SWAGGER_URL_V1
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.Group
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.badRequest
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.get
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.ok
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.responds
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.serviceUnavailable
import no.nav.altinn.admin.client.azuread.AzureAdClient
import no.nav.altinn.admin.client.wellknown.getWellKnown
import no.nav.altinn.admin.common.API_V1

fun Routing.loginAPI(environment: Environment) {
    getToken(environment)
    getToken2(environment)
    getLogin(environment)
    authenticate("auth-oauth-microsoft") {
        get("$API_V1/login") {}
        get("/oauth2/callback") {
            val principal: OAuthAccessTokenResponse.OAuth2? = call.principal()
            logger.debug { "access token: ${principal?.accessToken}" }
            logger.info { "Setting user session" }
            val userSession = UserSession(principal?.accessToken.toString())
            call.sessions.set(userSession)
            call.respondRedirect(SWAGGER_URL_V1)
        }
    }
}

internal data class AnError(val error: String)
internal const val GROUP_NAME = "Login"

private val logger = KotlinLogging.logger { }

@Group(GROUP_NAME)
@Location("$API_V1/login")
class Test

fun Routing.getLogin(environment: Environment) =
    get<Test> (
        "Login".responds(
            ok<String>(), serviceUnavailable<AnError>(), badRequest<AnError>()
        )
    ) {
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
