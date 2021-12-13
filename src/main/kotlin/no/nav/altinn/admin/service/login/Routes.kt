package no.nav.altinn.admin.service.login

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.sessions.clear
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import mu.KotlinLogging
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.Group
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.get
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.noContent
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.ok
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.responds
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.unAuthorized
import no.nav.altinn.admin.common.API_V1

@KtorExperimentalLocationsAPI
fun Routing.sessionAPI() {
    getToken()
    clear()
}

internal data class AnError(val error: String)
internal const val GROUP_NAME = "Session"

private val logger = KotlinLogging.logger { }

@KtorExperimentalLocationsAPI
@Group(GROUP_NAME)
@Location("$API_V1/session/token")
class Token

@KtorExperimentalLocationsAPI
fun Routing.getToken() =
    get<Token> (
        "Token".responds(ok<String>(), unAuthorized<String>())
    ) {
        val userSession: UserSession? = call.sessions.get<UserSession>()
        if (userSession != null) {
            logger.info { "Found UserSession, fetching token." }
            call.respond(HttpStatusCode.OK, userSession.idToken)
        } else {
            logger.info { "No UserSession, use login link at top of home page" }
            call.respond(HttpStatusCode.Unauthorized, "No UserSession found.")
        }
    }

@KtorExperimentalLocationsAPI
@Group(GROUP_NAME)
@Location("$API_V1/clear/session")
class ClearSession

@KtorExperimentalLocationsAPI
fun Routing.clear() =
    get<ClearSession> (
        "Clear".responds(
            ok<String>(), noContent<AnError>()
        )
    ) {
        call.sessions.clear<UserSession>()
        call.respond(HttpStatusCode.OK, "UserSession is cleared")
    }
