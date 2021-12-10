package no.nav.altinn.admin.service.login

import io.ktor.application.call
import io.ktor.client.HttpClient
import io.ktor.http.HttpStatusCode
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.response.respond
import io.ktor.routing.Routing
import mu.KotlinLogging
import no.nav.altinn.admin.Environment
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.Group
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.get
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.noContent
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.ok
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.responds
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.unAuthorized
import no.nav.altinn.admin.common.API_V1

@KtorExperimentalLocationsAPI
fun Routing.loginAPI(environment: Environment, httpClient: HttpClient) {
    getLogin(environment, httpClient)
    getLogout(environment)
}

internal data class AnError(val error: String)
internal const val GROUP_NAME = "Login"

private val logger = KotlinLogging.logger { }

@KtorExperimentalLocationsAPI
@Group(GROUP_NAME)
@Location("$API_V1/login")
class Login

@KtorExperimentalLocationsAPI
fun Routing.getLogin(environment: Environment, httpClient: HttpClient) =
    get<Login> (
        "Login".responds(ok<String>(), unAuthorized<String>())
    ) {
        val idToken = environment.azure.idToken
        if (idToken != "no-id-token") {
            logger.info { "A user is logged in." }
            call.respond(HttpStatusCode.OK, environment.azure.idToken)
        } else {
            logger.info { "User is not logged in, use login link at top of home page" }
            call.respond(HttpStatusCode.Unauthorized, "No user is logged in.")
        }
    }

@KtorExperimentalLocationsAPI
@Group(GROUP_NAME)
@Location("$API_V1/logout")
class Logout

@KtorExperimentalLocationsAPI
fun Routing.getLogout(environment: Environment) =
    get<Logout> (
        "Logout".responds(
            ok<String>(), noContent<AnError>()
        )
    ) {
        if (environment.azure.idToken == "no-id-token") {
            call.respond(HttpStatusCode.NoContent, AnError("No user logged in, no one to logout"))
            return@get
        }
        environment.azure.idToken = "no-id-token"
        call.respond(HttpStatusCode.OK, "User is logged out")
    }
