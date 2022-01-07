package no.nav.altinn.admin.service.receipt

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.locations.Location
import io.ktor.response.respond
import io.ktor.routing.Routing
import mu.KotlinLogging
import no.nav.altinn.admin.Environment
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.BearerTokenSecurity
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.Group
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.badRequest
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.get
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.ok
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.securityAndResponse
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.serviceUnavailable
import no.nav.altinn.admin.common.API_V1
import no.nav.altinn.admin.common.isDate

fun Routing.receiptsAPI(altinnReceiptService: AltinnReceiptService, environment: Environment) {
    getInboundReceipts(altinnReceiptService, environment)
    getCorresondenceReceipts(altinnReceiptService, environment)
    geBrokerServiceReceipts(altinnReceiptService, environment)
}

internal data class AnError(val error: String)
internal const val GROUP_NAME = "Receipts"

private val logger = KotlinLogging.logger { }

@Group(GROUP_NAME)
@Location("$API_V1/altinn/arkvitteringer/hent/{fraDato}/{tilDato}")
data class ArPeriode(val fraDato: String, val tilDato: String)

fun Routing.getInboundReceipts(altinnReceiptService: AltinnReceiptService, environment: Environment) =
    get<ArPeriode>(
        "Hent liste av mottatt Arkiv Referanser for periode".securityAndResponse(
            BearerTokenSecurity(), ok<ReceiptItems>(), serviceUnavailable<AnError>(), badRequest<AnError>()
        )
    ) {
        param ->

        val fraDato = param.fraDato.trim()
        val tilDato = param.tilDato.trim()
        if (fraDato.isNullOrEmpty() || tilDato.isNullOrEmpty()) {
            call.respond(HttpStatusCode.BadRequest, AnError("Mangler gyldig fra eller til dato for periode"))
            return@get
        }
        if (!isDate(fraDato) || !isDate(tilDato)) {
            call.respond(HttpStatusCode.BadRequest, AnError("Ugyldig fra eller til dato format [yyyy-mm-dd]"))
            return@get
        }
        if (fraDato > tilDato) {
            call.respond(HttpStatusCode.BadRequest, AnError("Fra dato kan ikke være større enn til dato"))
            return@get
        }

        try {
            val receipts = altinnReceiptService.getInboundReceipts(fraDato, tilDato)
            if (receipts.status == "Ok")
                call.respond(receipts)
            else
                call.respond(HttpStatusCode.NotFound, "Failed to get inbound receipts")
        } catch (ee: Exception) {
            logger.error {
                "IReceiptAgencyExternalBasic.GetReceiptListBasicV2 feilet  \n" +
                    "\n ErrorMessage  ${ee.message}" +
                    "\n LocalizedErrorMessage  ${ee.localizedMessage}"
            }
            call.respond(HttpStatusCode.InternalServerError, AnError("IReceiptAgencyExternalBasic.GetReceiptListBasicV2 feilet: ${ee.message}"))
        }
    }

@Group(GROUP_NAME)
@Location("$API_V1/altinn/meldingskvitteringer/hent/{fraDato}/{tilDato}")
data class MeldingsPeriode(val fraDato: String, val tilDato: String)

fun Routing.getCorresondenceReceipts(altinnReceiptService: AltinnReceiptService, environment: Environment) =
    get<MeldingsPeriode>(
        "Hent liste av sendte meldingskvitteringer for periode".securityAndResponse(
            BearerTokenSecurity(), ok<CorrespondenceReceiptItems>(), serviceUnavailable<AnError>(), badRequest<AnError>()
        )
    ) {
        param ->

        val fraDato = param.fraDato.trim()
        val tilDato = param.tilDato.trim()
        if (fraDato.isNullOrEmpty() || tilDato.isNullOrEmpty()) {
            call.respond(HttpStatusCode.BadRequest, AnError("Mangler gyldig fra eller til dato for periode"))
            return@get
        }
        if (!isDate(fraDato) || !isDate(tilDato)) {
            call.respond(HttpStatusCode.BadRequest, AnError("Ugyldig fra eller til dato format [yyyy-mm-dd]"))
            return@get
        }
        if (fraDato > tilDato) {
            call.respond(HttpStatusCode.BadRequest, AnError("Fra dato kan ikke være større enn til dato"))
            return@get
        }

        try {
            val receipts = altinnReceiptService.getCorrespondenceReceipts(fraDato, tilDato)
            if (receipts.status == "Ok")
                call.respond(receipts)
            else
                call.respond(HttpStatusCode.NotFound, "Failed to get correspondence receipts")
        } catch (ee: Exception) {
            logger.error {
                "IReceiptAgencyExternalBasic.GetReceiptListBasicV2 feilet  \n" +
                    "\n ErrorMessage  ${ee.message}" +
                    "\n LocalizedErrorMessage  ${ee.localizedMessage}"
            }
            call.respond(HttpStatusCode.InternalServerError, AnError("IReceiptAgencyExternalBasic.GetReceiptListBasicV2 feilet: ${ee.message}"))
        }
    }

@Group(GROUP_NAME)
@Location("$API_V1/altinn/formidlingskvitteringer/hent/{fraDato}/{tilDato}")
data class FormidlingsPeriode(val fraDato: String, val tilDato: String)

fun Routing.geBrokerServiceReceipts(altinnReceiptService: AltinnReceiptService, environment: Environment) =
    get<FormidlingsPeriode>(
        "Hent liste av sendte formidlingskvitteringer for periode".securityAndResponse(
            BearerTokenSecurity(), ok<CorrespondenceReceiptItems>(), serviceUnavailable<AnError>(), badRequest<AnError>()
        )
    ) {
        param ->

        val fraDato = param.fraDato.trim()
        val tilDato = param.tilDato.trim()
        if (fraDato.isNullOrEmpty() || tilDato.isNullOrEmpty()) {
            call.respond(HttpStatusCode.BadRequest, AnError("Mangler gyldig fra eller til dato for periode"))
            return@get
        }
        if (!isDate(fraDato) || !isDate(tilDato)) {
            call.respond(HttpStatusCode.BadRequest, AnError("Ugyldig fra eller til dato format [yyyy-mm-dd]"))
            return@get
        }
        if (fraDato > tilDato) {
            call.respond(HttpStatusCode.BadRequest, AnError("Fra dato kan ikke være større enn til dato"))
            return@get
        }

        try {
            val receipts = altinnReceiptService.getBrokerServiceReceipts(fraDato, tilDato)
            if (receipts.status == "Ok")
                call.respond(receipts)
            else
                call.respond(HttpStatusCode.NotFound, "Failed to get BrokerService receipts")
        } catch (ee: Exception) {
            logger.error {
                "IReceiptAgencyExternalBasic.GetReceiptListBasicV2 feilet  \n" +
                    "\n ErrorMessage  ${ee.message}" +
                    "\n LocalizedErrorMessage  ${ee.localizedMessage}"
            }
            call.respond(HttpStatusCode.InternalServerError, AnError("IReceiptAgencyExternalBasic.GetReceiptListBasicV2 feilet: ${ee.message}"))
        }
    }
