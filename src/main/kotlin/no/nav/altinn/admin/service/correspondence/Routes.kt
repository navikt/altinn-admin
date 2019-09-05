package no.nav.altinn.admin.service.correspondence

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.locations.Location
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.util.pipeline.PipelineContext
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
val regexDateFormat = """\d{4}-\d{2}-\d{2}${'$'}""".toRegex()

private val logger = KotlinLogging.logger { }
private fun isDate(date: String): Boolean = regexDateFormat.matches(date)

@Group(GROUP_NAME)
@Location("$API_V1/altinn/meldinger/hent/{tjenesteKode}/{fraDato}/{tilDato}/{avgiver}")
data class MeldingsFilter(val tjenesteKode: String, val fraDato: String = "2019-12-24", val tilDato: String = "2019-12-24", val avgiver: String?)

fun Routing.getCorrespondenceFiltered(altinnCorrespondenceService: AltinnCorrespondenceService, environment: Environment) =
    get<MeldingsFilter>("Hent status på filtrerte meldinger fra en meldingstjeneste".securityAndReponds(BasicAuthSecurity(),
        ok<CorrespondenceDetails>(), serviceUnavailable<AnError>(), badRequest<AnError>())) {
        param ->

        if (notValidServiceCode(param.tjenesteKode, environment)) return@get

        val fromDate = param.fraDato
        val toDate = param.tilDato
        if (fromDate.isNullOrEmpty() || toDate.isNullOrEmpty()) {
            call.respond(HttpStatusCode.BadRequest, AnError("Fra eller til dato på filter er ikke oppgitt"))
            return@get
        }
        if (!isDate(fromDate)) {
            call.respond(HttpStatusCode.BadRequest, AnError("Fra dato er oppgitt i feil format, bruk yyyy-mm-dd"))
            return@get
        }
        if (!isDate(toDate)) {
            call.respond(HttpStatusCode.BadRequest, AnError("Til dato er oppgitt i feil format, bruk yyyy-mm-dd"))
            return@get
        }
        try {
            val fraDato = LocalDateTime.parse(fromDate, DateTimeFormat.forPattern("yyyy-MM-dd")).toDateTime()
            val tilDato = LocalDateTime.parse(toDate, DateTimeFormat.forPattern("yyyy-MM-dd")).toDateTime()
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
    get<TjenesteKode>("Hent status meldinger fra en meldingstjeneste".securityAndReponds(BasicAuthSecurity(),
        ok<CorrespondenceDetails>(), serviceUnavailable<AnError>(), badRequest<AnError>())) {
        param ->

        if (notValidServiceCode(param.tjenesteKode, environment)) return@get
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

private suspend fun PipelineContext<Unit, ApplicationCall>.notValidServiceCode(tjenesteKode: String, environment: Environment): Boolean {
    if (tjenesteKode.isEmpty()) {
        call.respond(HttpStatusCode.BadRequest, AnError("Tjeneste kode kan ikke være tom"))
        return true
    }
    val corrList = environment.correspondeceService.serviceCodes.split(",")
    if (!corrList.contains(tjenesteKode)) {
        call.respond(HttpStatusCode.BadRequest, AnError("Ugyldig tjeneste kode oppgitt"))
        return true
    }
    return false
}