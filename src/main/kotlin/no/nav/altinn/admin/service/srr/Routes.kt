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
import no.nav.altinn.admin.Environment
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.BasicAuthSecurity
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.Group
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.badRequest
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.delete
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.get
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.ok
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.post
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.responds
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.securityAndReponds
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.serviceUnavailable
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.unAuthorized
import no.nav.altinn.admin.common.API_V1
import no.nav.altinn.admin.ldap.LDAPAuthenticate

fun Routing.ssrAPI(altinnSrrService: AltinnSRRService, environment: Environment) {
    getRightsList(altinnSrrService)
    getRightsForReportee(altinnSrrService)
    addRightsForReportee(altinnSrrService, environment)
    deleteRightsForReportee(altinnSrrService, environment)
}

internal data class AnError(val error: String)
internal const val GROUP_NAME = "Rettighetsregister"

private val logger = KotlinLogging.logger { }

@Group(GROUP_NAME)
@Location("$API_V1/altinn/rettighetsregister/hent")
class Rettighetsregister

fun Routing.getRightsList(altinnSrrService: AltinnSRRService) =
    get<Rettighetsregister>("hent rettigheter for alle virksomheter".responds(ok<RightsResponse>(), serviceUnavailable<AnError>(), badRequest<AnError>())) {
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

@Group(GROUP_NAME)
@Location("$API_V1/altinn/rettighetsregister/hent/{orgnr}")
data class FirmaRettigheter(val orgnr: String)

fun Routing.getRightsForReportee(altinnSrrService: AltinnSRRService) =
    get<FirmaRettigheter>("hent rettigheter for en virksomhet".responds(ok<RightsResponse>(), serviceUnavailable<AnError>(), badRequest<AnError>())) {
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

@Group(GROUP_NAME)
@Location("$API_V1/altinn/rettighetsregister/leggtil")
class PostLeggTilRettighet
data class PostLeggTilRettighetBody(val orgnr: String, val lesEllerSkriv: String, val domene: String)

fun Routing.addRightsForReportee(altinnSrrService: AltinnSRRService, environment: Environment) =
        post<PostLeggTilRettighet, PostLeggTilRettighetBody> ("Legg til rettighet for en virksomhet"
                .securityAndReponds(BasicAuthSecurity(), ok<RightsResponse>(), serviceUnavailable<AnError>(), badRequest<AnError>(), unAuthorized<Unit>())
        ) { _, body ->
            val currentUser = call.principal<UserIdPrincipal>()!!.name
            val logEntry = "Bruker $currentUser legger til rettighet til virksomhet  - $body"
            application.environment.log.info(logEntry)

            LDAPAuthenticate(environment.application).getUsersGroupNames(currentUser)
//            val userExist = try {
//                LdapA(application.environment).use { ldap -> ldap.userExists(currentUser) }
//            } catch (e: Exception) { false }
//
//            if (!userExist) {
//                val msg = "authenticated user $currentUser doesn't exist as NAV ident or " +
//                        "service user in current LDAP domain, or ldap unreachable, cannot be manager of topic"
//                application.environment.log.warn(msg)
//                call.respond(HttpStatusCode.ServiceUnavailable, AnError(msg))
//                return@post
//            }

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

@Group(GROUP_NAME)
@Location("$API_V1/altinn/rettighetsregister/slett")
data class DeleteRettighet(val orgnr: String, val lesEllerSkriv: String, val domene: String)

fun Routing.deleteRightsForReportee(altinnSrrService: AltinnSRRService, environment: Environment) =
        delete<DeleteRettighet> ("Slett rettighet for en virksomhet"
                .securityAndReponds(BasicAuthSecurity(), ok<RightsResponse>(), serviceUnavailable<AnError>(), badRequest<AnError>(), unAuthorized<Unit>())
        ) { param ->
            val currentUser = call.principal<UserIdPrincipal>()!!.name
            val logEntry = "Sletter rettighet for en virksomhet $currentUser - $param"
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
                return@delete
            }

            val virksomhetsnummer = param.orgnr
            if (!virksomhetsnummer.isBlank() && virksomhetsnummer.length != 9) {
                call.respond(HttpStatusCode.BadRequest, AnError("Ikke gyldig virksomhetsnummer"))
                return@delete
            }

            var srrType = RegisterSRRRightsType.READ
            if (param.lesEllerSkriv.equals("skriv", true)) {
                srrType = RegisterSRRRightsType.WRITE
            }

            if (param.domene.isNullOrEmpty()) {
                call.respond(HttpStatusCode.BadRequest, AnError("Ikke gyldig domene"))
                return@delete
            }

            val rightResponse = altinnSrrService.deleteRights(virksomhetsnummer, param.domene, srrType)
            call.respond(HttpStatusCode.OK, rightResponse)
        }
