package no.nav.altinn.admin.service.owner

import com.fasterxml.jackson.module.kotlin.readValue
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
import io.ktor.client.statement.readText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.parseQueryString
import io.ktor.locations.Location
import io.ktor.response.respond
import io.ktor.routing.Routing
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.altinn.admin.Environment
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.Group
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.badRequest
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.ok
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.post
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.responds
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.serviceUnavailable
import no.nav.altinn.admin.client.MaskinportenClient
import no.nav.altinn.admin.common.API_V1
import no.nav.altinn.admin.common.objectMapper

fun Routing.ownerApi(maskinporten: MaskinportenClient, environment: Environment) {
    getReportees(maskinporten, environment)
    getRights(maskinporten, environment)
}

internal data class AnError(val error: String)

internal const val GROUP_NAME = "Serviceowner api"
internal const val ALTINN_BASE_URL = "https://tt02.altinn.no/"
internal const val PAGE = 500

private val logger = KotlinLogging.logger { }

internal val defaultHttpClient = HttpClient(CIO) {
    install(JsonFeature) {
        serializer = JacksonSerializer { objectMapper }
    }
    install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.HEADERS
    }
}

@Group(GROUP_NAME)
@Location("$API_V1/serviceowner/reportee")
class Filter
data class FilterBody(val apikey: String, val subject: String, val sc: String?, val sec: String?)

// subject={subject}&serviceCode={serviceCode}&serviceEition={serviceEdition}&roleDefinitionId={roleDefinitionId}&showConsentReportees={showConsentReportees}
fun Routing.getReportees(maskinporten: MaskinportenClient, environment: Environment) =
    post<Filter, FilterBody>(
        "Hent reportees på subject".responds(
            ok<List<Reportee>>(), serviceUnavailable<AnError>(), badRequest<AnError>()
        )
    ) { param, body ->

        if (body.apikey.isEmpty()) {
            call.respond(HttpStatusCode.BadRequest, AnError("Mangler gyldig apikey"))
            return@post
        }
        if (body.subject.isEmpty() || body.subject.length != 11) {
            call.respond(HttpStatusCode.BadRequest, AnError("Mangler gyldig subject"))
            return@post
        }
        var token = ""
        runBlocking {
            token = maskinporten.tokenRequest()
        }
        if (token.isNullOrEmpty()) {
            call.respond(HttpStatusCode.Unauthorized, AnError("No access token"))
            return@post
        }
        var output = mutableListOf<Reportee>()
        var skip = 0
        do {
            defaultHttpClient.request<HttpStatement>(ALTINN_BASE_URL + "api/serviceowner/reportees") {
                method = HttpMethod.Get
                header("ApiKey", body.apikey)
                header("Authorization", "Bearer $token")
                header("Accept", "application/hal+json")
                parameter("subject", body.subject)
                if (!body.sc.isNullOrBlank()) {
                    parameter("serviceCode", body.sc)
                }
                if (!body.sec.isNullOrBlank()) {
                    parameter("serviceEdition", body.sec)
                }
                parameter("top", "$PAGE")
                if (skip != 0) {
                    parameter("skip", skip)
                }
            }.execute { response: HttpResponse ->

                if (response.status != HttpStatusCode.OK) {
                    logger.warn { "Error response from $ALTINN_BASE_URL request: $response" }
                } else {
                    val outputt = objectMapper.readValue<List<Reportee>>(response.readBytes())
                    skip += outputt.size
                    output.addAll(outputt)
                    logger.info { "Got a response, size is ${outputt.size}, skip is $skip" }
                }
            }
        } while (skip > 0 && skip % PAGE == 0 && skip < 5000)
        call.respond(output)
    }

@Group(GROUP_NAME)
@Location("$API_V1/serviceowner/authorization/rights")
class Rights
data class RightsBody(val apikey: String, val subject: String, val repotee: String)

// subject={subject}&serviceCode={serviceCode}&serviceEition={serviceEdition}&roleDefinitionId={roleDefinitionId}&showConsentReportees={showConsentReportees}
fun Routing.getRights(maskinporten: MaskinportenClient, environment: Environment) =
    post<Rights, RightsBody>(
        "Hent rights på subject og reportee".responds(
            ok<ReporteeList>(), serviceUnavailable<AnError>(), badRequest<AnError>()
        )
    ) { param, body ->

        if (body.apikey.isEmpty()) {
            call.respond(HttpStatusCode.BadRequest, AnError("Mangler gyldig apikey"))
            return@post
        }
        if (body.subject.isEmpty()) {
            call.respond(HttpStatusCode.BadRequest, AnError("Mangler gyldig subject"))
            return@post
        }
        if (body.repotee.isEmpty()) {
            call.respond(HttpStatusCode.BadRequest, AnError("Mangler gyldig reportee"))
            return@post
        }
        var token = ""
        runBlocking {
            token = maskinporten.tokenRequest()
        }
        if (token.isEmpty()) {
            call.respond(HttpStatusCode.Unauthorized, AnError("No access token"))
            return@post
        }
        var output = ""
        defaultHttpClient.request<HttpStatement>(ALTINN_BASE_URL + "api/serviceowner/authorization/rights") {
            method = HttpMethod.Get
            header("ApiKey", body.apikey)
            header("Authorization", "Bearer $token")
            header("Accept", "application/hal+json")
            parameter("subject", body.subject)
            parameter("reportee", body.repotee)
        }.execute { response: HttpResponse ->

            if (response.status != HttpStatusCode.OK) {
                logger.warn { "Error response from $ALTINN_BASE_URL request: $response" }
                output = "Failed"
            } else {
                // val outputt = objectMapper.readValue(response.readBytes(), ReporteeList::class.java)
                output = response.readText()
                logger.debug { "Is this a token (length): ${output.length}" }
            }
        }
        call.respond(output)
    }
