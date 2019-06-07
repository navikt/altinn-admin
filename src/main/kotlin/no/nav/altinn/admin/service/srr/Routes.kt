package no.nav.altinn.admin.service.srr

import io.ktor.application.application
import io.ktor.application.call
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.principal
import io.ktor.http.HttpStatusCode
import io.ktor.locations.Location
import io.ktor.response.respond
import io.ktor.routing.*
import mu.KotlinLogging
import no.altinn.schemas.services.register._2015._06.RegisterSRRRightsType
import no.altinn.services.register.srr._2015._06.IRegisterSRRAgencyExternalBasicGetRightsBasicAltinnFaultFaultFaultMessage
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.BasicAuthSecurity
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.Group
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.badRequest
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.get
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.ok
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.post
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.responds
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.securityAndReponds
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.serviceUnavailable
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.unAuthorized
import no.nav.altinn.admin.common.API_V1

fun Routing.ssrAPI(altinnSrrService: AltinnSRRService) {
    getRightsList(altinnSrrService)
    getRightsForReportee(altinnSrrService)
    addRightsForReportee(altinnSrrService)
}

internal data class AnError(val error: String)

private val logger = KotlinLogging.logger { }

@Group("Register")
@Location("$API_V1/altinn/rettighetsregister/hent")
class Rettighetsregister

fun Routing.getRightsList(altinnSrrService: AltinnSRRService) =
    get<Rettighetsregister>("hent rettigheter for alle virksomheter".responds(ok<AltinnSRRService>(), serviceUnavailable<AnError>(), badRequest<AnError>())) {
        try {
            val rightsResponse = altinnSrrService.getRightsForAllBusinesses()
            call.respond(HttpStatusCode.OK, rightsResponse)
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

@Group("Register")
@Location("$API_V1/altinn/rettighetsregister/hent/{orgnr}")
data class FirmaRettigheter(val orgnr: String)

fun Routing.getRightsForReportee(altinnSrrService: AltinnSRRService) =
    get<FirmaRettigheter>("hent rettigheter for en virksomhet".responds(ok<AltinnSRRService>(), serviceUnavailable<AnError>(), badRequest<AnError>())) {
        param ->
        val virksomhetsnummer: String? = param.orgnr
        try {
            if (!virksomhetsnummer.isNullOrBlank() && virksomhetsnummer.length == 9) {
                val rightResponse = altinnSrrService.getRightsForABusiness(virksomhetsnummer)
                call.respond(HttpStatusCode.OK, rightResponse)
            } else {
                call.respond(HttpStatusCode.BadRequest, AnError("Feil virksomhetsnummer $virksomhetsnummer."))
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

@Group("Register")
@Location("$API_V1/altinn/rettighetsregister/leggtil")
class PostLeggTilRettighet
data class PostLeggTilRettighetBody(val orgnr: String, val lesEllerSkriv: String, val domene: String)

fun Routing.addRightsForReportee(altinnSrrService: AltinnSRRService) =
        post<PostLeggTilRettighet, PostLeggTilRettighetBody> ("Legg til rettighet for en virksomhet"
                .securityAndReponds(BasicAuthSecurity(), ok<PostT>, serviceUnavailable<AnError>(), badRequest<AnError>(), unAuthorized<Unit>())
        ) { _, body ->
            val currentUser = call.principal<UserIdPrincipal>()!!.name
            val logEntry = "Topic creation request by $currentUser - $body"
            application.environment.log.info(logEntry)

            val userExist = true
//            try {
//                LDAPGroup(fasitConfig).use { ldap -> ldap.userExists(currentUser) }
//            } catch (e: Exception) { false }

            if (!userExist) {
                val msg = "authenticated user $currentUser doesn't exist as NAV ident or " +
                        "service user in current LDAP domain, or ldap unreachable, cannot be manager of topic"
                application.environment.log.warn(msg)
                call.respond(HttpStatusCode.ServiceUnavailable, AnError(msg))
                return@post
            }

            val virksomhetsnummer = body.orgnr
            if (!virksomhetsnummer.isBlank() && virksomhetsnummer.length != 9) {
                call.respond(HttpStatusCode.BadRequest, AnError("Ikke gyldig virksomhetsnummer"))
                return@post
            }

            var srrType = RegisterSRRRightsType.READ
            if (body.lesEllerSkriv.equals("skriv", true)) {
                srrType = RegisterSRRRightsType.WRITE
            }

            if (body.domene.isNullOrEmpty()) {
                call.respond(HttpStatusCode.BadRequest, AnError("Ikke gyldig domene"))
                return@post
            }

            val rightResponse = altinnSrrService.addRights(virksomhetsnummer, body.domene, srrType)
            call.respond(HttpStatusCode.OK, rightResponse)
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
