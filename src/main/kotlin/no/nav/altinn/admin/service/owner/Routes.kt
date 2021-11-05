package no.nav.altinn.admin.service.owner

import io.ktor.application.call
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.logging.DEFAULT
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logger
import io.ktor.client.features.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.HttpStatement
import io.ktor.client.statement.readBytes
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.locations.Location
import io.ktor.response.respond
import io.ktor.routing.Routing
import mu.KotlinLogging
import no.nav.altinn.admin.Environment
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.Group
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.badRequest
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.get
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.ok
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.responds
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.serviceUnavailable
import no.nav.altinn.admin.client.MaskinportenClient
import no.nav.altinn.admin.common.API_V1
import no.nav.altinn.admin.common.objectMapper

fun Routing.ownerApi(maskinporten: MaskinportenClient, environment: Environment) {
    getReportees(maskinporten, environment)
}

internal data class AnError(val error: String)

internal const val GROUP_NAME = "Serviceowner api"
internal const val ALTINN_BASE_URL = "https://tt02.altinn.no/"

private val logger = KotlinLogging.logger { }

internal val defaultHttpClient = HttpClient(CIO) {
    install(JsonFeature) {
        serializer = JacksonSerializer { objectMapper }
    }
    install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.NONE
    }
}

@Group(GROUP_NAME)
@Location("$API_V1/serviceowner/reportee/{apikey}/{subject}/{sc}/{sec}")
data class Filter(val apikey: String, val subject: String, val sc: String?, val sec: String?)

// subject={subject}&serviceCode={serviceCode}&serviceEition={serviceEdition}&roleDefinitionId={roleDefinitionId}&showConsentReportees={showConsentReportees}
fun Routing.getReportees(maskinporten: MaskinportenClient, environment: Environment) =
    get<Filter>(
        "Hent reportees på subject".responds(
            ok<ReporteeList>(), serviceUnavailable<AnError>(), badRequest<AnError>()
        )
    ) { param ->

        if (param.apikey.isEmpty()) {
            call.respond(HttpStatusCode.BadRequest, AnError("Mangler gyldig apikey"))
            return@get
        }
        if (param.subject.isEmpty() || param.subject.length != 11) {
            call.respond(HttpStatusCode.BadRequest, AnError("Mangler gyldig subject"))
            return@get
        }
        val token = maskinporten.tokenRequest()
        if (token.isNullOrEmpty()) {
            call.respond(HttpStatusCode.Unauthorized, AnError("No access token"))
            return@get
        }
        var output = ""
        logger.info { "Try so api..." }
        defaultHttpClient.request<HttpStatement>(ALTINN_BASE_URL + "/api/serviceowner/reportees?") {
            method = HttpMethod.Get
            header("ApiKey", param.apikey)
            header("Authorization", "Bearer $token")
            header("Accept", "application/hal+json")
            parameter("subject", param.subject)
            if (!param.sc.isNullOrBlank()) {
                parameter("serviceCode", param.sc)
            }
            if (!param.sec.isNullOrBlank()) {
                parameter("serviceEdition", param.sec)
            }
        }.execute { response: HttpResponse ->

            if (response.status != HttpStatusCode.OK) {
                logger.warn { "Error response from $ALTINN_BASE_URL request: $response" }
                output = "Failed"
            } else {
                output = objectMapper.readValue(response.readBytes(), String::class.java)
                logger.debug { "Is this a token (length): ${output.length}" }
            }
        }
        call.respond(output)
    }
