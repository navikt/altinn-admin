package no.nav.altinn.admin.service.srr

import io.ktor.application.application
import io.ktor.application.call
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.principal
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.locations.Location
import io.ktor.response.header
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
import java.io.File
import java.io.FileWriter

fun Routing.ssrAPI(altinnSrrService: AltinnSRRService, environment: Environment) {
    getRightsList(altinnSrrService, environment)
    getRightsForReportee(altinnSrrService, environment)
    addRightsForReportee(altinnSrrService, environment)
    deleteRightsForReportee(altinnSrrService, environment)
    getTullmessage(altinnSrrService, environment)
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
        val scList = environment.application.serviceCodes.split(",")
        if (!scList.contains(param.tjenesteKode)) {
            call.respond(HttpStatusCode.BadRequest, AnError("Ugyldig tjeneste kode oppgitt"))
            return@get
        }

        try {
            val rightsResponse = altinnSrrService.getRightsForAllBusinesses(param.tjenesteKode)
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

        val scList = environment.application.serviceCodes.split(",")
        if (!scList.contains(param.tjenesteKode)) {
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

                val scList = environment.application.serviceCodes.split(",")
                if (!scList.contains(body.tjenesteKode)) {
                    call.respond(HttpStatusCode.BadRequest, AnError("Ugyldig tjeneste kode oppgitt"))
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

                val rightResponse = altinnSrrService.addRights(body.tjenesteKode, virksomhetsnummer, body.domene, srrType)
                if (rightResponse.status == "Failed") {
                    call.respond(HttpStatusCode.BadRequest, AnError(rightResponse.message))
                    return@post
                }
                call.respond(HttpStatusCode.OK, rightResponse)
            }

@Group(GROUP_NAME)
@Location("$API_V1/altinn/rettighetsregister/slett/{tjenesteKode}/{orgnr}/{lesEllerSkriv}/{domene}")
data class DeleteRettighet(val tjenesteKode: String, val orgnr: String, val lesEllerSkriv: String, val domene: String)

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

            val scList = environment.application.serviceCodes.split(",")
            if (!scList.contains(param.tjenesteKode)) {
                call.respond(HttpStatusCode.BadRequest, AnError("Ugyldig tjeneste kode oppgitt"))
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

            val rightResponse = altinnSrrService.deleteRights(param.tjenesteKode, virksomhetsnummer, param.domene, srrType)
            if (rightResponse.status == "Failed") {
                call.respond(HttpStatusCode.BadRequest, AnError(rightResponse.message))
                return@delete
            }
            call.respond(HttpStatusCode.OK, rightResponse)
        }

@Group(GROUP_NAME)
@Location("$API_V1/altinn/rettighetsregister/tull")
class TullReferanse

fun Routing.getTullmessage(altinnDqService: AltinnSRRService, environment: Environment) =
    get<TullReferanse>("hent AR melding fra dq".responds(ok<Any>(), badRequest<Any>())) {
        call.response.header(HttpHeaders.ContentType, "application/xml")
        try {
            logger.info { "Create file" }
            var file = File.createTempFile("temp", ".xml")
            FileWriter(file).write("Some text")
            logger.info { "Written some text to file ${file.absolutePath} : ${file.toURI().toURL()}" }
//            call.response.header(HttpHeaders.ContentDisposition, ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, "${file.absolutePath}").toString())
            call.response.header(HttpHeaders.ContentDisposition, "attachment; filename=\"${file.absolutePath}\"")

            call.respond(HttpStatusCode.OK)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, AnError(e.message.toString()))
        }
    }