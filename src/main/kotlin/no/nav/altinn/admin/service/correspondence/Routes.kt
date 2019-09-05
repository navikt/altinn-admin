package no.nav.altinn.admin.service.correspondence

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.locations.Location
import io.ktor.response.respond
import io.ktor.routing.Routing
import mu.KotlinLogging
import no.nav.altinn.admin.Environment
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.BasicAuthSecurity
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.Group
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.badRequest
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.get
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.ok
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.securityAndReponds
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.serviceUnavailable
import no.nav.altinn.admin.common.API_V1
import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat

fun Routing.correspondenceAPI(altinnCorrespondenceService: AltinnCorrespondenceService, environment: Environment) {
    getCorrespondence(altinnCorrespondenceService, environment)
    getCorrespondenceFiltered(altinnCorrespondenceService, environment)
}

internal data class AnError(val error: String)
internal const val GROUP_NAME = "Correspondence"

private val logger = KotlinLogging.logger { }

@Group(GROUP_NAME)
@Location("$API_V1/altinn/meldinger/hent/{tjenesteKode}/{fraDato}/{tilDato}/{avgiver}")
data class MeldingsFilter(val tjenesteKode: String, val fraDato: String = "2019-12-24", val tilDato: String = "2019-12-24", val avgiver: String?)

fun Routing.getCorrespondenceFiltered(altinnCorrespondenceService: AltinnCorrespondenceService, environment: Environment) =
    get<MeldingsFilter>("Hent melding fra en Arkiv Referanse via DQ".securityAndReponds(BasicAuthSecurity(),
        ok<AnError>(), serviceUnavailable<AnError>(), badRequest<AnError>())) {
        param ->

        if (param.tjenesteKode.isNullOrEmpty()) {
            call.respond(HttpStatusCode.BadRequest, AnError("Tjeneste kode kan ikke være tom"))
            return@get
        }
        val corrList = environment.correspondeceService.serviceCodes.split(",")
        if (!corrList.contains(param.tjenesteKode)) {
            call.respond(HttpStatusCode.BadRequest, AnError("Ugyldig tjeneste kode oppgitt"))
            return@get
        }
        val fraDato = LocalDateTime.parse(param.fraDato, DateTimeFormat.forPattern("yyyy-MM-dd")).toDateTime()
        val tilDato = LocalDateTime.parse(param.tilDato, DateTimeFormat.forPattern("yyyy-MM-dd")).toDateTime()

        try {
            val correspondenceResponse = altinnCorrespondenceService.getCorrespondenceDetails(param.tjenesteKode, fraDato, tilDato, param.avgiver)

            if (correspondenceResponse.status == "Ok")
                call.respond(correspondenceResponse.correspondenceDetails)
            else
                call.respond(HttpStatusCode.NotFound, correspondenceResponse.message)
        } catch (ee: Exception) {
            logger.error {
                "IDownloadQueueExternalBasic.GetArchivedFormTaskBasicDQ feilet  \n" +
                    "\n ErrorMessage  ${ee.message}" +
                    "\n LocalizedErrorMessage  ${ee.localizedMessage}"
            }
            call.respond(HttpStatusCode.InternalServerError, AnError("IDownloadQueueExternalBasic.GetArchivedFormTaskBasicDQ feilet: ${ee.message}"))
        }
    }

@Group(GROUP_NAME)
@Location("$API_V1/altinn/meldinger/hent/{tjenesteKode}")
data class TjenesteKode(val tjenesteKode: String)

fun Routing.getCorrespondence(altinnCorrespondenceService: AltinnCorrespondenceService, environment: Environment) =
    get<TjenesteKode>("Hent melding fra en Arkiv Referanse via DQ".securityAndReponds(BasicAuthSecurity(),
        ok<AnError>(), serviceUnavailable<AnError>(), badRequest<AnError>())) {
        param ->

        if (param.tjenesteKode.isNullOrEmpty()) {
            call.respond(HttpStatusCode.BadRequest, AnError("Tjeneste kode kan ikke være tom"))
            return@get
        }
        val corrList = environment.correspondeceService.serviceCodes.split(",")
        if (!corrList.contains(param.tjenesteKode)) {
            call.respond(HttpStatusCode.BadRequest, AnError("Ugyldig tjeneste kode oppgitt"))
            return@get
        }

        try {
            val correspondenceResponse = altinnCorrespondenceService.getCorrespondenceDetails(param.tjenesteKode)

            if (correspondenceResponse.status == "Ok")
                call.respond(correspondenceResponse.correspondenceDetails)
            else
                call.respond(HttpStatusCode.NotFound, correspondenceResponse.message)
        } catch (ee: Exception) {
            logger.error {
                "IDownloadQueueExternalBasic.GetArchivedFormTaskBasicDQ feilet  \n" +
                    "\n ErrorMessage  ${ee.message}" +
                    "\n LocalizedErrorMessage  ${ee.localizedMessage}"
            }
            call.respond(HttpStatusCode.InternalServerError, AnError("IDownloadQueueExternalBasic.GetArchivedFormTaskBasicDQ feilet: ${ee.message}"))
        }
    }