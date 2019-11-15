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
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.ParameterInputType
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

fun Routing.ssrAPI(altinnSrrService: AltinnSRRService, environment: Environment) {
    getRightsList(altinnSrrService, environment)
    getRightsListServiceEdition(altinnSrrService, environment)
    getRightsForReportee(altinnSrrService, environment)
    getRightsForReporteeServiceEdition(altinnSrrService, environment)
    addRightsForReportee(altinnSrrService, environment)
    deleteRightsForReportee(altinnSrrService, environment)
}

internal data class AnError(val error: String)
internal const val GROUP_NAME = "Rettighetsregister"

private val logger = KotlinLogging.logger { }

@Group(GROUP_NAME)
@Location("$API_V1/altinn/rettighetsregister/hent/{tjenesteKode}")
data class Rettighetsregister(val tjenesteKode: String)

fun Routing.getRightsList(altinnSrrService: AltinnSRRService, environment: Environment) =
    get<Rettighetsregister>("hent rettigheter for alle virksomheter".responds(ok<RegistryResponse>(), serviceUnavailable<AnError>(), badRequest<AnError>())) {
        param ->
        val scList = filterOutServiceCode(environment, param.tjenesteKode)
        if (scList.size == 0) {
            call.respond(HttpStatusCode.BadRequest, AnError("Ugyldig tjeneste kode oppgitt"))
            return@get
        }

        try {
            val srr = mutableListOf<RegistryResponse.Register>()
            scList.forEach {
                val rightsResponse = altinnSrrService.getRightsForAllBusinesses(it.first, it.second)
                if (rightsResponse.status == "Ok")
                    srr.addAll(rightsResponse.register.register)
            }
            if (srr.size > 0)
                call.respond(HttpStatusCode.OK, RegistryResponse(srr))
            else
                call.respond(HttpStatusCode.NotFound, "Did not find any rights, check log for more information")
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
@Location("$API_V1/altinn/rettighetsregister/hent/{tjenesteKode}/{utgaveKode}")
data class RettighetsregisterUtgave(val tjenesteKode: String, val utgaveKode: String)

fun Routing.getRightsListServiceEdition(altinnSrrService: AltinnSRRService, environment: Environment) =
    get<RettighetsregisterUtgave>("hent rettigheter for alle virksomheter".responds(ok<RegistryResponse>(), serviceUnavailable<AnError>(), badRequest<AnError>())) {
        param ->
        val scList = filterOutServiceCode(environment, param.tjenesteKode)
        if (scList.size == 0) {
            call.respond(HttpStatusCode.BadRequest, AnError("Ugyldig tjeneste kode oppgitt"))
            return@get
        }

        try {
            val rightsResponse = altinnSrrService.getRightsForAllBusinesses(param.tjenesteKode, param.utgaveKode)
            if (rightsResponse.status == "Ok")
                call.respond(rightsResponse.register)
            else
                call.respond(HttpStatusCode.NotFound, rightsResponse.message)
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
@Location("$API_V1/altinn/rettighetsregister/hent/{tjenesteKode}/{orgnr}")
data class FirmaRettigheter(val tjenesteKode: String, val orgnr: String)

fun Routing.getRightsForReportee(altinnSrrService: AltinnSRRService, environment: Environment) =
    get<FirmaRettigheter>("hent rettigheter for en virksomhet".responds(ok<RegistryResponse>(), serviceUnavailable<AnError>(), badRequest<AnError>())) {
        param ->
        val virksomhetsnummer: String? = param.orgnr
        val scList = filterOutServiceCode(environment, param.tjenesteKode)
        if (scList.size == 0) {
            call.respond(HttpStatusCode.BadRequest, AnError("Ugyldig tjeneste kode oppgitt"))
            return@get
        }

        try {
            if (!virksomhetsnummer.isNullOrBlank() && virksomhetsnummer.length == 9) {
                val rightsResponse = altinnSrrService.getRightsForABusiness(param.tjenesteKode, virksomhetsnummer)
                if (rightsResponse.status == "Ok")
                    call.respond(rightsResponse.register)
                else
                    call.respond(HttpStatusCode.NotFound, rightsResponse.message)
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
@Location("$API_V1/altinn/rettighetsregister/hent/{tjenesteKode}/{utgaveKode}/{orgnr}")
data class FirmaRettigheterUtgave(val tjenesteKode: String, val utgaveKode: String, val orgnr: String)

fun Routing.getRightsForReporteeServiceEdition(altinnSrrService: AltinnSRRService, environment: Environment) =
    get<FirmaRettigheterUtgave>("hent rettigheter for en virksomhet".responds(ok<RegistryResponse>(), serviceUnavailable<AnError>(), badRequest<AnError>())) {
        param ->
        val virksomhetsnummer: String? = param.orgnr

        val scList = filterOutServiceCode(environment, param.tjenesteKode)
        if (scList.size == 0) {
            call.respond(HttpStatusCode.BadRequest, AnError("Ugyldig tjeneste kode oppgitt"))
            return@get
        }

        try {
            if (!virksomhetsnummer.isNullOrBlank() && virksomhetsnummer.length == 9) {
                val rightsResponse = altinnSrrService.getRightsForABusiness(param.tjenesteKode, virksomhetsnummer, param.utgaveKode)
                if (rightsResponse.status == "Ok")
                    call.respond(rightsResponse.register)
                else
                    call.respond(HttpStatusCode.NotFound, rightsResponse.message)
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

fun Routing.addRightsForReportee(altinnSrrService: AltinnSRRService, environment: Environment) =
            post<PostLeggTilRettighet, PostLeggTilRettighetBody> ("Legg til rettighet for en virksomhet"
                    .securityAndReponds(BasicAuthSecurity(), ok<RightsResponse>(), serviceUnavailable<AnError>(), badRequest<AnError>(), unAuthorized<Unit>())
            ) { _, body ->
                val currentUser = call.principal<UserIdPrincipal>()!!.name

                val approvedUsers = environment.application.users.split(",")
                val userExist = approvedUsers.contains(currentUser)
                if (!userExist) {
                    val msg = "Autentisert bruker $currentUser eksisterer ikke i listen for godkjente brukere."
                    application.environment.log.warn(msg)
                    call.respond(HttpStatusCode.ServiceUnavailable, AnError(msg))
                    return@post
                }

                val logEntry = "Bruker $currentUser forsøker å legge til rettighet til virksomhet  - ${ParameterInputType.body}"
                application.environment.log.info(logEntry)

                val scList = filterOutServiceCode(environment, body.tjenesteKode)
                if (scList.size == 0) {
                    call.respond(HttpStatusCode.BadRequest, AnError("Ugyldig tjeneste kode oppgitt"))
                    return@post
                }

                if (body.utgaveKode.trim().isEmpty() || body.utgaveKode.toIntOrNull() == null) {
                    call.respond(HttpStatusCode.BadRequest, AnError("Ikke gyldig utgaveKode"))
                    return@post
                }

                val virksomhetsnummer = body.orgnr
                if (virksomhetsnummer.isBlank() || virksomhetsnummer.length != 9) {
                    call.respond(HttpStatusCode.BadRequest, AnError("Ikke gyldig virksomhetsnummer"))
                    return@post
                }

                var srrType = RegisterSRRRightsType.READ
                if ("skriv" != body.lesEllerSkriv && "les" != body.lesEllerSkriv) {
                    call.respond(HttpStatusCode.BadRequest, AnError("lesEllerSkriv verdien må være enten 'les' eller 'skriv'"))
                    return@post
                }
                if (body.lesEllerSkriv.equals("skriv", true)) {
                    srrType = RegisterSRRRightsType.WRITE
                }

                if (body.domene.isEmpty()) {
                    call.respond(HttpStatusCode.BadRequest, AnError("Ikke gyldig domene"))
                    return@post
                }

                val rightResponse = altinnSrrService.addRights(body.tjenesteKode, body.utgaveKode, virksomhetsnummer, body.domene, srrType)
                if (rightResponse.status == "Failed") {
                    call.respond(HttpStatusCode.BadRequest, AnError(rightResponse.message))
                    return@post
                }
                call.respond(HttpStatusCode.OK, rightResponse)
            }

@Group(GROUP_NAME)
@Location("$API_V1/altinn/rettighetsregister/slett/{tjenesteKode}/{utgaveKode}/{orgnr}/{lesEllerSkriv}/{domene}")
data class DeleteRettighet(val tjenesteKode: String, val utgaveKode: String, val orgnr: String, val lesEllerSkriv: String, val domene: String)

fun Routing.deleteRightsForReportee(altinnSrrService: AltinnSRRService, environment: Environment) =
        delete<DeleteRettighet> ("Slett rettighet for en virksomhet"
                .securityAndReponds(BasicAuthSecurity(), ok<RightsResponse>(), serviceUnavailable<AnError>(), badRequest<AnError>(), unAuthorized<Unit>())
        ) { param ->
            val currentUser = call.principal<UserIdPrincipal>()!!.name
            val approvedUsers = environment.application.users.split(",")
            val userExist = approvedUsers.contains(currentUser)
            if (!userExist) {
                val msg = "Autentisert bruker $currentUser eksisterer ikke i listen for godkjente brukere."
                application.environment.log.warn(msg)
                call.respond(HttpStatusCode.ServiceUnavailable, AnError(msg))
                return@delete
            }

            val logEntry = "Forsøker å slette en rettighet for en virksomhet $currentUser - $param"
            application.environment.log.info(logEntry)

            val scList = filterOutServiceCode(environment, param.tjenesteKode)
            if (scList.size == 0) {
                call.respond(HttpStatusCode.BadRequest, AnError("Ugyldig tjeneste kode oppgitt"))
                return@delete
            }

            if (param.utgaveKode.trim().isEmpty() || param.utgaveKode.toIntOrNull() == null) {
                call.respond(HttpStatusCode.BadRequest, AnError("Ikke gyldig utgaveKode"))
                return@delete
            }

            val virksomhetsnummer = param.orgnr
            if (virksomhetsnummer.isBlank() || virksomhetsnummer.length != 9) {
                call.respond(HttpStatusCode.BadRequest, AnError("Ikke gyldig virksomhetsnummer"))
                return@delete
            }

            var srrType = RegisterSRRRightsType.READ
            if ("skriv" != param.lesEllerSkriv && "les" != param.lesEllerSkriv) {
                call.respond(HttpStatusCode.BadRequest, AnError("lesEllerSkriv verdien må være enten 'les' eller 'skriv'"))
                return@delete
            }

            if (param.lesEllerSkriv.equals("skriv", true)) {
                srrType = RegisterSRRRightsType.WRITE
            }

            if (param.domene.trim().isEmpty()) {
                call.respond(HttpStatusCode.BadRequest, AnError("Ikke gyldig domene"))
                return@delete
            }

            val rightResponse = altinnSrrService.deleteRights(param.tjenesteKode, param.utgaveKode, virksomhetsnummer, param.domene, srrType)
            if (rightResponse.status == "Failed") {
                call.respond(HttpStatusCode.BadRequest, AnError(rightResponse.message))
                return@delete
            }
            call.respond(HttpStatusCode.OK, rightResponse)
        }

private fun filterOutServiceCode(environment: Environment, tjenesteKode: String): MutableList<Pair<String, String>> {
    val scSecList = environment.srrService.serviceCodes.split(",")
    val scList = mutableListOf<Pair<String, String>>()
    scSecList.forEach {
        val sc = it.split(":")
        if (sc[0] == tjenesteKode)
            scList.add(Pair(sc[0], if (sc.size > 1) sc[1] else "1"))
    }
    return scList
}
