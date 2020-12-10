package no.nav.altinn.admin.service.prefill

import io.ktor.application.application
import io.ktor.application.call
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.principal
import io.ktor.http.HttpStatusCode
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.response.respond
import io.ktor.routing.Routing
import java.time.ZonedDateTime
import java.util.GregorianCalendar
import javax.xml.datatype.DatatypeFactory
import mu.KotlinLogging
import no.altinn.schemas.serviceengine.formsengine._2009._10.TransportType
import no.altinn.schemas.services.serviceengine.notification._2009._10.Notification
import no.altinn.schemas.services.serviceengine.notification._2009._10.NotificationBEList
import no.altinn.schemas.services.serviceengine.notification._2009._10.ReceiverEndPoint
import no.altinn.schemas.services.serviceengine.notification._2009._10.ReceiverEndPointBEList
import no.altinn.schemas.services.serviceengine.notification._2009._10.TextToken
import no.altinn.schemas.services.serviceengine.notification._2009._10.TextTokenSubstitutionBEList
import no.altinn.schemas.services.serviceengine.prefill._2009._10.PrefillForm
import no.altinn.schemas.services.serviceengine.prefill._2009._10.PrefillFormBEList
import no.altinn.schemas.services.serviceengine.prefill._2009._10.PrefillFormTask
import no.nav.altinn.admin.Environment
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.BasicAuthSecurity
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.Group
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.badRequest
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.ok
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.post
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.securityAndReponds
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.serviceUnavailable
import no.nav.altinn.admin.common.API_V1
import no.nav.altinn.admin.common.dateTimeToXmlGregorianCalendar
import no.nav.altinn.admin.common.dateToXmlGregorianCalendar
import no.nav.altinn.admin.common.isDate
import no.nav.altinn.admin.common.randomUuid
import no.nav.altinn.admin.common.toXmlGregorianCalendar

@KtorExperimentalLocationsAPI
fun Routing.prefillAPI(altinnPrefillService: AltinnPrefillService, environment: Environment) {
    postPrefill(altinnPrefillService, environment)
//    postFile(altinnCorrespondenceService, environment)
}

internal data class AnError(val error: String)

internal const val GROUP_NAME = "Prefill"

private val logger = KotlinLogging.logger { }

@KtorExperimentalLocationsAPI
@Group(GROUP_NAME)
@Location("$API_V1/altinn/prefill/send/tjeneste")
class Prefill

@KtorExperimentalLocationsAPI
fun Routing.postPrefill(altinnPrefillService: AltinnPrefillService, environment: Environment) =
    post<Prefill, PostPrefillBody>(
        "Hent status på filtrerte meldinger fra en meldingstjeneste".securityAndReponds(
            BasicAuthSecurity(), ok<PrefillResponse>(),
            serviceUnavailable<AnError>(), badRequest<AnError>()
        )
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
        val dueDate = body.duedate
        if (dueDate.isNullOrEmpty()) {
            call.respond(HttpStatusCode.BadRequest, AnError("Due dato er ikke oppgitt"))
            return@post
        }
        if (!isDate(dueDate)) {
            call.respond(HttpStatusCode.BadRequest, AnError("Due dato er oppgitt i feil format, bruk yyyy-mm-dd, eller yyyy-mm-ddThh:mm:ss"))
            return@post
        }
        val fristDato = if (isDate(dueDate)) dateToXmlGregorianCalendar(dueDate) else dateTimeToXmlGregorianCalendar(dueDate)

        val scList = filterOutServiceCode(environment, body.tjeneste.servicecode)
        if (scList.size == 0) {
            call.respond(HttpStatusCode.BadRequest, AnError("Ugyldig tjeneste kode oppgitt"))
            return@post
        }

        if (!scList.contains(Pair(body.tjeneste.servicecode, body.tjeneste.serviceeditioncode))) {
            call.respond(HttpStatusCode.BadRequest, AnError("Ugyldig tjeneste utgave kode oppgitt"))
            return@post
        }

        val prefillFormTask = PrefillFormTask()
        var notifications: NotificationBEList? = null
        if (body.varsel != null) {
            notifications = getNotification(body.varsel)
        }
        prefillFormTask.externalServiceCode = body.tjeneste.servicecode
        prefillFormTask.externalServiceEditionCode = body.tjeneste.serviceeditioncode.toInt()
        prefillFormTask.externalShipmentReference = "NAV-" + randomUuid()
        prefillFormTask.serviceOwnerCode = "NAV"
        prefillFormTask.prefillNotifications = notifications
        prefillFormTask.reportee = body.orgnr

        prefillFormTask.preFillForms = PrefillFormBEList().apply {
            prefillForm.add(
                PrefillForm().apply {
                    dataFormatID = body.tjeneste.dataformatid
                    dataFormatVersion = body.tjeneste.dataformatversion
                    formDataXML = body.melding
                    sendersReference = ""
                    isSignedByDefault = true
                    isSigningLocked = true
                }
            )
        }

        prefillFormTask.validFromDate = DatatypeFactory.newInstance()
            .newXMLGregorianCalendar(GregorianCalendar.from(ZonedDateTime.now()))
        prefillFormTask.validToDate = DatatypeFactory.newInstance()
            .newXMLGregorianCalendar(GregorianCalendar.from(ZonedDateTime.now().plusDays(30)))

        try {
            val meldingsResponse = altinnPrefillService.prefillFormTask(prefillFormTask, fristDato)
            call.respond(meldingsResponse)
        } catch (ee: Exception) {
            logger.error {
                "iPrefillExternalBasic.prefillFormTask feilet  \n" +
                    "\n ErrorMessage  ${ee.message}" +
                    "\n LocalizedErrorMessage  ${ee.localizedMessage}"
            }
            call.respond(HttpStatusCode.InternalServerError, AnError("iPrefillExternalBasic.prefillFormTask feilet: ${ee.message}"))
        }
    }

fun getNotification(varsel: List<Varsel>): NotificationBEList {
    val notificationBEList = NotificationBEList()
    varsel.forEach { v ->
        val notification = Notification()
        notification.textTokens = TextTokenSubstitutionBEList()
        notification.fromAddress = v.fraAdresse
        notification.languageCode = "1044"
        notification.shipmentDateTime = toXmlGregorianCalendar(v.forsendelseDatoTid)
        notification.notificationType = v.varselType.toString()
        val tittelToken = TextToken()
        tittelToken.tokenNum = 0
        tittelToken.tokenValue = if (v.varselType == VarselType.TokenTextOnly) v.tittel else v.melding
        notification.textTokens.textToken.add(tittelToken)
        val meldingToken = TextToken()
        meldingToken.tokenNum = 1
        meldingToken.tokenValue = if (v.varselType == VarselType.TokenTextOnly) v.melding else ""
        notification.textTokens.textToken.add(meldingToken)
        notification.receiverEndPoints = ReceiverEndPointBEList()
        v.ekstraMottakere.forEach {
            val receiverEndPoint = ReceiverEndPoint()
            receiverEndPoint.receiverAddress = it.mottakerAdresse
            receiverEndPoint.transportType = TransportType.fromValue(it.forsendelseType.toString())
            notification.receiverEndPoints.receiverEndPoint.add(receiverEndPoint)
        }
        notificationBEList.notification.add(notification)
    }

    return notificationBEList
}

private fun filterOutServiceCode(environment: Environment, tjenesteKode: String): MutableList<Pair<String, String>> {
    val scSecList = environment.prefillService.serviceCodes.split(",")
    val scList = mutableListOf<Pair<String, String>>()
    scSecList.forEach {
        val sc = it.split(":")
        if (sc[0] == tjenesteKode)
            scList.add(Pair(sc[0], if (sc.size > 1) sc[1] else "1"))
    }
    return scList
}
