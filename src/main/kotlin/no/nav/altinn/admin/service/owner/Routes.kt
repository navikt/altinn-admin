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
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
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
import no.nav.altinn.admin.service.srr.SrrType

fun Routing.ownerApi(maskinporten: MaskinportenClient, environment: Environment) {
    getReportees(maskinporten, environment)
    getRights(maskinporten, environment)
    getSrr(maskinporten, environment)
}

internal data class AnError(val error: String)

internal const val GROUP_NAME = "Serviceowner api"
internal const val ALTINN_BASE_URL = "https://tt02.altinn.no/"
internal const val PAGE = 50

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
@Location("$API_V1/serviceowner/reportee")
class Filter
data class FilterBody(val apikey: String, val subject: String, val sc: String?, val sec: String?)

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
                parameter("\$top", PAGE)
                parameter("\$skip", skip)
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
data class RightsBody(val apikey: String, val subject: String, val reportee: String)

fun Routing.getRights(maskinporten: MaskinportenClient, environment: Environment) =
    post<Rights, RightsBody>(
        "Hent rights på subject og reportee".responds(
            ok<RightsResponse>(), serviceUnavailable<AnError>(), badRequest<AnError>()
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
        if (body.reportee.isEmpty()) {
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
        var output: RightsResponse? = null
        var skip = 0
        do {
            defaultHttpClient.request<HttpStatement>(ALTINN_BASE_URL + "api/serviceowner/authorization/rights") {
                method = HttpMethod.Get
                header("ApiKey", body.apikey)
                header("Authorization", "Bearer $token")
                header("Accept", "application/hal+json")
                parameter("subject", body.subject)
                parameter("reportee", body.reportee)
                parameter("\$top", PAGE)
                parameter("\$skip", skip)
            }.execute { response: HttpResponse ->

                if (response.status != HttpStatusCode.OK) {
                    logger.warn { "Error response from $ALTINN_BASE_URL request: $response" }
                } else {
                    // val outputt = objectMapper.readValue(response.readBytes(), ReporteeList::class.java)
                    val rightsResponse = objectMapper.readValue<RightsResponse>(response.readBytes())
                    skip += rightsResponse.rights.size
                    if (output == null) {
                        output = rightsResponse
                    } else {
                        output?.rights?.addAll(rightsResponse.rights)
                    }
                    logger.info { "Got a response, size is ${rightsResponse.rights.size}, skip is $skip" }
                }
            }
        } while (skip > 0 && skip % PAGE == 0 && skip < 5000)
        call.respond(output!!)
    }

@Group(GROUP_NAME)
@Location("$API_V1/serviceowner/srr")
class SRR
data class SrrBody(val apikey: String, val srr: SrrType, val reportee: String?)

fun Routing.getSrr(maskinporten: MaskinportenClient, environment: Environment) =
    post<SRR, SrrBody>(
        "Hent info fra tjenesteeier styrt rettighetsregister på tjenesten".responds(
            ok<List<SrrResponse>>(), serviceUnavailable<AnError>(), badRequest<AnError>()
        )
    ) { param, body ->

        if (body.apikey.isEmpty()) {
            call.respond(HttpStatusCode.BadRequest, AnError("Mangler gyldig apikey"))
            return@post
        }
        if (body.srr.servicecode.isEmpty()) {
            call.respond(HttpStatusCode.BadRequest, AnError("Mangler gyldig serviceCode"))
            return@post
        }
        if (body.srr.serviceeditioncode.isEmpty()) {
            call.respond(HttpStatusCode.BadRequest, AnError("Mangler gyldig serviceEditionCode"))
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
        val output = mutableListOf<SrrResponse>()
        var skip = 0
        do {
            defaultHttpClient.request<HttpStatement>(ALTINN_BASE_URL + "api/serviceowner/Srr") {
                method = HttpMethod.Get
                header("ApiKey", body.apikey)
                header("Authorization", "Bearer $token")
                header("Accept", "application/hal+json")
                parameter("serviceCode", body.srr.servicecode)
                parameter("serviceEditionCode", body.srr.serviceeditioncode)
                if (!body.reportee.isNullOrEmpty()) {
                    parameter("reportee", body.reportee)
                }
                parameter("\$top", PAGE)
                parameter("\$skip", skip)
            }.execute { response: HttpResponse ->

                if (response.status != HttpStatusCode.OK) {
                    logger.warn { "Error response from $ALTINN_BASE_URL request: $response" }
                } else {

                    val srrResponse = objectMapper.readValue<List<SrrResponse>>(response.readBytes())
                    skip += srrResponse.size
                    output.addAll(srrResponse)
                    logger.info { "Got a response, size is ${srrResponse.size}, skip is $skip" }
                }
            }
        } while (skip > 0 && skip % PAGE == 0 && skip < 5000)
        call.respond(output!!)
    }
