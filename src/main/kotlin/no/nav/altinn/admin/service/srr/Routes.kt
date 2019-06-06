package no.nav.altinn.admin.service.srr

import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.locations.Location
import io.ktor.response.respond
import io.ktor.routing.*
import mu.KotlinLogging
import no.altinn.services.register.srr._2015._06.IRegisterSRRAgencyExternalBasicGetRightsBasicAltinnFaultFaultFaultMessage
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.Group
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.badRequest
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.get
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.ok
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.responds
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.serviceUnavailable
import no.nav.altinn.admin.common.API_V1

@Group("Register")
@Location("$API_V1/altinn/rettighetsregister/hent")
class Rettighetsregister

fun Routing.api(altinnSrrService: AltinnSRRService) {
    authenticate {
        this@api.altinnsrrserviceGet(altinnSrrService)
    }
}

internal data class AnError(val error: String)

private val logger = KotlinLogging.logger { }

fun Routing.altinnsrrserviceGet(altinnSrrService: AltinnSRRService) =
    get<Rettighetsregister>("hent rettigheter".responds(ok<AltinnSRRService>(), serviceUnavailable<AnError>(), badRequest<AnError>())) {
    val orgnr: String? = call.request.queryParameters["orgnr"]
    try {
        if (!orgnr.isNullOrBlank() && orgnr.length == 9) {
            val rightResponse = altinnSrrService.getRightsForABusiness(orgnr)
            call.respond(HttpStatusCode.OK, rightResponse)
        } else if (orgnr.isNullOrBlank()) {
            val rightsResponse = altinnSrrService.getRightsForAllBusinesses()
            call.respond(HttpStatusCode.OK, rightsResponse)
        } else {
            call.respond(HttpStatusCode.BadRequest, AnError("Feil virksomhetsnummer $orgnr."))
        }
    } catch (e: IRegisterSRRAgencyExternalBasicGetRightsBasicAltinnFaultFaultFaultMessage) {
        logger.error {
            "IRegisterSRRAgencyExternalBasic.GetRightsBasic feilet \n" +
                    "\n ErrorMessage  ${e.faultInfo.altinnErrorMessage}" +
                    "\n ExtendedErrorMessage  ${e.faultInfo.altinnExtendedErrorMessage}" +
                    "\n LocalizedErrorMessage  ${e.faultInfo.altinnLocalizedErrorMessage}" +
                    "\n ErrorGuid  ${e.faultInfo.errorGuid}" +
                    "\n UserGuid  ${e.faultInfo.userGuid}" +
                    "\n UserId  ${e.faultInfo.userId}"
        }
        call.respond(HttpStatusCode.InternalServerError, AnError("IRegisterSRRAgencyExternalBasic.GetRightsBasic feilet"))
    } catch (ee: Exception) {
        logger.error {
            "IRegisterSRRAgencyExternalBasic.GetRightsBasic feilet  \n" +
                    "\n ErrorMessage  ${ee.message}" +
                    "\n LocalizedErrorMessage  ${ee.localizedMessage}"
        }
        call.respond(HttpStatusCode.InternalServerError, AnError("IRegisterSRRAgencyExternalBasic.GetRightsBasic feilet"))
    }
}

//  route("/altinn/rettighetsregister/leggtil") {
//      post {
//          val rr = call.receive<RequestRegister>()
//          if (!rr.organisasjonsnummer.isBlank() && rr.organisasjonsnummer.length == 9) {
//              val rightResponse = if (rr.rettighet == "write")
//                      altinnSrrService.addRights(rr.organisasjonsnummer, rr.domene, RegisterSRRRightsType.WRITE)
//                  else
//                      altinnSrrService.addRights(rr.organisasjonsnummer, rr.domene, RegisterSRRRightsType.READ)
//              var ok = HttpStatusCode.OK
//              if (!rightResponse.status.equals("OK", true)) ok = HttpStatusCode.BadRequest
//
//              call.respond(ok, rightResponse)
//          } else {
//              call.respond(HttpStatusCode.BadRequest, "Ikke gyldig virksomhetsnummer")
//          }
//      }
//  }
//  route("/altinn/rettighetsregister/fjern") {
//      post {
//          val rr = call.receive<RequestRegister>()
//          if (!rr.organisasjonsnummer.isBlank() && rr.organisasjonsnummer.length == 9) {
//              val rightResponse = if (rr.rettighet == "write")
//                  altinnSrrService.deleteRights(rr.organisasjonsnummer, rr.domene, RegisterSRRRightsType.WRITE)
//              else
//                  altinnSrrService.deleteRights(rr.organisasjonsnummer, rr.domene, RegisterSRRRightsType.READ)
//
//              var ok = HttpStatusCode.OK
//              if (!rightResponse.status.equals("OK", true)) ok = HttpStatusCode.BadRequest
//              call.respond(ok, rightResponse)
//          } else {
//              call.respond(HttpStatusCode.BadRequest, "Ikke gyldig virksomhetsnummer")
//          }
//      }
//  }
