package no.nav.altinn.admin.service.login

import io.ktor.application.call
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.locations.Location
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.routing.Routing
import io.ktor.sessions.clear
import io.ktor.sessions.sessions
import mu.KotlinLogging
import no.nav.altinn.admin.Environment
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.Group
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.badRequest
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.get
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.ok
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.responds
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.serviceUnavailable
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.unAuthorized
import no.nav.altinn.admin.client.wellknown.getWellKnown
import no.nav.altinn.admin.common.API_V1

fun Routing.loginAPI(environment: Environment, httpClient: HttpClient) {
    // getToken(environment)
    // getToken2(environment)
    getLogin(environment, httpClient)
    getLogout()
}

internal data class AnError(val error: String)
internal const val GROUP_NAME = "Login"

private val logger = KotlinLogging.logger { }

@Group(GROUP_NAME)
@Location("$API_V1/login")
class Login

fun Routing.getLogin(environment: Environment, httpClient: HttpClient) =
    get<Login> (
        "Login".responds(
            ok<LoginInfo>(), serviceUnavailable<AnError>(), unAuthorized<AnError>()
        )
    ) {
        httpClient.get<Any>("https://altinn-admin.dev.intern.nav.no/oauth2/login")
//        val userSession: UserSession? = call.sessions.get<UserSession>()
//        val principal: OAuthAccessTokenResponse.OAuth2? = call.principal()
//        if (userSession != null) {
//            logger.info { "Got usersession here, principal is $principal" }
//            call.respond(HttpStatusCode.OK, LoginInfo("Copy token and paste it as bearer token", environment.azure.accesstoken))
//        } else {
//            logger.info { "usersession is null" }
//            call.respond(HttpStatusCode.Unauthorized, AnError("Could not authorize, try again"))
//        }
    }

@Group(GROUP_NAME)
@Location("$API_V1/logout")
class Logout

fun Routing.getLogout() =
    get<Logout> (
        "Logout".responds(
            ok<String>(), serviceUnavailable<AnError>(), unAuthorized<AnError>()
        )
    ) {
        call.sessions.clear<UserSession>()
        call.respondRedirect("/")
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
