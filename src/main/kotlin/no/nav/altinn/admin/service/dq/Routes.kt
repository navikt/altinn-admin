package no.nav.altinn.admin.service.dq

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
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.delete
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.get
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.ok
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.securityAndReponds
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.serviceUnavailable
import no.nav.altinn.admin.common.API_V1

fun Routing.dqAPI(altinnDqService: AltinnDQService, environment: Environment) {
    getFormMessage(altinnDqService, environment)
    getDqItems(altinnDqService, environment)
    getDqItemsSec(altinnDqService, environment)
    logger.info { "Local env ? ${environment.application.localEnv}" }
    if (environment.application.localEnv != "prod") {
        purgeItem(altinnDqService, environment)
    }
}

internal data class AnError(val error: String)
internal const val GROUP_NAME = "DownloadQueue"

private val logger = KotlinLogging.logger { }

@Group(GROUP_NAME)
@Location("$API_V1/altinn/dq/hent/{arNummer}")
data class ArkivReferanse(val arNummer: String)

fun Routing.getFormMessage(altinnDqService: AltinnDQService, environment: Environment) =
    get<ArkivReferanse>("Hent melding fra en Arkiv Referanse via DQ".securityAndReponds(BasicAuthSecurity(),
        ok<ArData>(), serviceUnavailable<AnError>(), badRequest<AnError>())) {
        param ->

        val arNummer = param.arNummer.trim()
        if (arNummer.isNullOrEmpty()) {
            call.respond(HttpStatusCode.BadRequest, AnError("Ugyldig AR nummer oppgitt"))
            return@get
        }

        try {
            val dqResponse = altinnDqService.getFormData(arNummer)
            if (dqResponse.status == "Ok")
                call.respond(dqResponse.arData)
            else
                call.respond(HttpStatusCode.NotFound, dqResponse.message)
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
@Location("$API_V1/altinn/dq/elementer/{tjenesteKode}")
data class TjenesteKode(val tjenesteKode: String)

fun Routing.getDqItems(altinnDqService: AltinnDQService, environment: Environment) =
    get<TjenesteKode>("Hent elementer som ligger på download queue".securityAndReponds(
        BasicAuthSecurity(), ok<DqItems>(), serviceUnavailable<AnError>(), badRequest<AnError>())) {
        param ->

        val tjenesteKode = param.tjenesteKode.trim()
        if (tjenesteKode.isNullOrEmpty()) {
            call.respond(HttpStatusCode.BadRequest, AnError("Blank tjeneste kode oppgitt"))
            return@get
        }

        val dqList = environment.dqService.serviceCodes.split(",")
        if (!dqList.contains(tjenesteKode)) {
            call.respond(HttpStatusCode.BadRequest, AnError("Ugyldig tjeneste kode oppgitt"))
            return@get
        }

        try {
            val dqResponse = altinnDqService.getDownloadQueueItems(tjenesteKode, "")
            call.respond(dqResponse)
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
@Location("$API_V1/altinn/dq/elementer/{tjenesteKode}/{utgaveKode}")
data class TjenesteOgUtgaveKode(val tjenesteKode: String, val utgaveKode: String)

fun Routing.getDqItemsSec(altinnDqService: AltinnDQService, environment: Environment) =
    get<TjenesteOgUtgaveKode>("Hent elementer som ligger på download queue filtrert med utgave kode".securityAndReponds(
        BasicAuthSecurity(), ok<DqItems>(), serviceUnavailable<AnError>(), badRequest<AnError>())) {
        param ->

        val tjenesteKode = param.tjenesteKode.trim()
        if (tjenesteKode.isNullOrEmpty()) {
            call.respond(HttpStatusCode.BadRequest, AnError("Blank tjeneste kode oppgitt"))
            return@get
        }

        val utgaveKode = param.utgaveKode.trim()
        if (utgaveKode.isNullOrEmpty()) {
            call.respond(HttpStatusCode.BadRequest, AnError("Blank utgave kode oppgitt"))
            return@get
        }

        val dqList = environment.dqService.serviceCodes.split(",")
        if (!dqList.contains(tjenesteKode)) {
            call.respond(HttpStatusCode.BadRequest, AnError("Ugyldig tjeneste kode oppgitt"))
            return@get
        }

        try {
            val dqResponse = altinnDqService.getDownloadQueueItems(tjenesteKode, utgaveKode)
            call.respond(dqResponse)
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
@Location("$API_V1/altinn/dq/slett/{arNummer}")
data class DeleteArkivReferanse(val arNummer: String)

fun Routing.purgeItem(altinnDqService: AltinnDQService, environment: Environment) =
    delete<DeleteArkivReferanse>("Slett AR fra download queue".securityAndReponds(
        BasicAuthSecurity(), ok<DqPurge>(), serviceUnavailable<AnError>(), badRequest<AnError>())) {
        param ->

        val arNummer = param.arNummer.trim()
        if (arNummer.isNullOrEmpty()) {
            call.respond(HttpStatusCode.BadRequest, AnError("Ugyldig AR nummer oppgitt"))
            return@delete
        }

        try {
            val dqResponse = altinnDqService.purgeItem(arNummer)
            call.respond(dqResponse)
        } catch (ee: Exception) {
            logger.error {
                "IDownloadQueueExternalBasic.GetArchivedFormTaskBasicDQ feilet  \n" +
                    "\n ErrorMessage  ${ee.message}" +
                    "\n LocalizedErrorMessage  ${ee.localizedMessage}"
            }
            call.respond(HttpStatusCode.InternalServerError, AnError("IDownloadQueueExternalBasic.GetArchivedFormTaskBasicDQ feilet: ${ee.message}"))
        }
    }
