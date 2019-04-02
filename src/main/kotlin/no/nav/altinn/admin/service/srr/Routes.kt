package no.nav.altinn.admin.service.srr

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import mu.KotlinLogging
import no.altinn.schemas.services.register._2015._06.RegisterSRRRightsType
import no.altinn.services.register.srr._2015._06.IRegisterSRRAgencyExternalBasicGetRightsBasicAltinnFaultFaultFaultMessage

private val logger = KotlinLogging.logger { }

fun Route.altinnsrrservice (
    altinnSrrService: AltinnSRRService
) {
    route("/altinn/rettighetsregister/hent") {
        get {
            val orgnr : String? = call.request.queryParameters["orgnr"]
            try {
                if (!orgnr.isNullOrBlank() && orgnr.length == 9) {
                    val rightResponse = altinnSrrService.getRightsForABusiness(orgnr)
                    call.respond(HttpStatusCode.OK, rightResponse)
                }
                else if (orgnr.isNullOrBlank()) {
                    val rightsResponse = altinnSrrService.getRightsForAllBusinesses()
                    call.respond(HttpStatusCode.OK, rightsResponse)
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Feil virksomhetsnummer $orgnr.")
                }
            } catch (e : IRegisterSRRAgencyExternalBasicGetRightsBasicAltinnFaultFaultFaultMessage)
            {
                logger.error {
                    "IRegisterSRRAgencyExternalBasic.GetRightsBasic feilet \n" +
                            "\n ErrorMessage  ${e.faultInfo.altinnErrorMessage}" +
                            "\n ExtendedErrorMessage  ${e.faultInfo.altinnExtendedErrorMessage}" +
                            "\n LocalizedErrorMessage  ${e.faultInfo.altinnLocalizedErrorMessage}" +
                            "\n ErrorGuid  ${e.faultInfo.errorGuid}" +
                            "\n UserGuid  ${e.faultInfo.userGuid}" +
                            "\n UserId  ${e.faultInfo.userId}"
                }
                call.respond(HttpStatusCode.InternalServerError, "IRegisterSRRAgencyExternalBasic.GetRightsBasic feilet")

            } catch (ee : Exception) {
                logger.error {
                    "IRegisterSRRAgencyExternalBasic.GetRightsBasic feilet  \n" +
                            "\n ErrorMessage  ${ee.message}" +
                            "\n LocalizedErrorMessage  ${ee.localizedMessage}"
                }
                call.respond(HttpStatusCode.InternalServerError, "IRegisterSRRAgencyExternalBasic.GetRightsBasic feilet")
            }
        }
    }
    route("/altinn/rettighetsregister/leggtil") {
        post {
            val rr = call.receive<RequestRegister>()
            if (!rr.organisasjonsnummer.isBlank() && rr.organisasjonsnummer.length == 9) {
                val rightResponse = if (rr.rettighet == "write")
                        altinnSrrService.addRights(rr.organisasjonsnummer, rr.domene, RegisterSRRRightsType.WRITE)
                    else
                        altinnSrrService.addRights(rr.organisasjonsnummer, rr.domene, RegisterSRRRightsType.READ)
                var ok = HttpStatusCode.OK
                if (!rightResponse.status.equals("OK", true)) ok = HttpStatusCode.BadRequest

                call.respond(ok, rightResponse)
            }
            else {
                call.respond(HttpStatusCode.BadRequest, "Ikke gyldig virksomhetsnummer")
            }
        }
    }
    route("/altinn/rettighetsregister/fjern") {
        post {
            val rr = call.receive<RequestRegister>()
            if (!rr.organisasjonsnummer.isBlank() && rr.organisasjonsnummer.length == 9) {
                val rightResponse = if (rr.rettighet == "write")
                    altinnSrrService.deleteRights(rr.organisasjonsnummer, rr.domene, RegisterSRRRightsType.WRITE)
                else
                    altinnSrrService.deleteRights(rr.organisasjonsnummer, rr.domene, RegisterSRRRightsType.READ)

                var ok = HttpStatusCode.OK
                if (!rightResponse.status.equals("OK", true)) ok = HttpStatusCode.BadRequest
                call.respond(ok, rightResponse)
            }
            else {
                call.respond(HttpStatusCode.BadRequest, "Ikke gyldig virksomhetsnummer")
            }
        }
    }
}
