package no.nav.altinn.admin.service.correspondence

import io.ktor.application.ApplicationCall
import io.ktor.application.application
import io.ktor.application.call
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.principal
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.locations.Location
import io.ktor.request.ApplicationRequest
import io.ktor.request.contentType
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.util.pipeline.PipelineContext
import mu.KotlinLogging
import no.altinn.schemas.serviceengine.formsengine._2009._10.TransportType
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.AttachmentsV2
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.ExternalContentV2
import no.altinn.schemas.services.serviceengine.notification._2009._10.Notification
import no.altinn.schemas.services.serviceengine.notification._2009._10.NotificationBEList
import no.altinn.schemas.services.serviceengine.notification._2009._10.ReceiverEndPoint
import no.altinn.schemas.services.serviceengine.notification._2009._10.ReceiverEndPointBEList
import no.altinn.schemas.services.serviceengine.notification._2009._10.TextToken
import no.altinn.schemas.services.serviceengine.notification._2009._10.TextTokenSubstitutionBEList
import no.altinn.schemas.services.serviceengine.subscription._2009._10.AttachmentFunctionType
import no.altinn.services.serviceengine.reporteeelementlist._2010._10.BinaryAttachmentExternalBEV2List
import no.altinn.services.serviceengine.reporteeelementlist._2010._10.BinaryAttachmentV2
import no.nav.altinn.admin.Environment
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.BasicAuthSecurity
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.Group
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.badRequest
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.get
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.ok
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.post
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.securityAndReponds
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.serviceUnavailable
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.unAuthorized
import no.nav.altinn.admin.common.API_V1
import no.nav.altinn.admin.common.decodeBase64
import no.nav.altinn.admin.common.isDate
import no.nav.altinn.admin.common.toXmlGregorianCalendar

fun Routing.correspondenceAPI(altinnCorrespondenceService: AltinnCorrespondenceService, environment: Environment) {
    getCorrespondence(altinnCorrespondenceService, environment)
    getCorrespondenceFiltered(altinnCorrespondenceService, environment)
    postCorrespondence(altinnCorrespondenceService, environment)
//    postFile(altinnCorrespondenceService, environment)
}

internal data class AnError(val error: String)
internal const val GROUP_NAME = "Correspondence"

private val logger = KotlinLogging.logger { }

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
            val fraDato = toXmlGregorianCalendar(fromDate)
            val tilDato = toXmlGregorianCalendar(toDate)
            val correspondenceResponse = altinnCorrespondenceService.getCorrespondenceDetails(param.tjenesteKode, fraDato, tilDato, param.avgiver)

            if (correspondenceResponse.status == "Ok")
                call.respond(correspondenceResponse.correspondenceDetails)
            else
                call.respond(HttpStatusCode.NotFound, correspondenceResponse.message)
        } catch (ee: Exception) {
            logger.error {
                "iCorrespondenceExternalBasic.getCorrespondenceDetails feilet  \n" +
                    "\n ErrorMessage  ${ee.message}" +
                    "\n LocalizedErrorMessage  ${ee.localizedMessage}"
            }
            call.respond(HttpStatusCode.InternalServerError, AnError("iCorrespondenceExternalBasic.getCorrespondenceDetails feilet: ${ee.message}"))
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
                "iCorrespondenceExternalBasic.getCorrespondenceDetails feilet \n" +
                    "\n ErrorMessage  ${ee.message}" +
                    "\n LocalizedErrorMessage  ${ee.localizedMessage}"
            }
            call.respond(HttpStatusCode.InternalServerError, AnError("iCorrespondenceExternalBasic.getCorrespondenceDetails feilet: ${ee.message}"))
        }
    }

@Group(GROUP_NAME)
@Location("$API_V1/altinn/meldinger/send")
class SendMelding

fun Routing.postCorrespondence(altinnCorrespondenceService: AltinnCorrespondenceService, environment: Environment) =
    post<SendMelding, PostCorrespondenceBody> ("Send melding til virksomhet"
        .securityAndReponds(BasicAuthSecurity(), ok<CorrespondenceResponse>(), serviceUnavailable<AnError>(), badRequest<AnError>(), unAuthorized<Unit>())
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

        if (notValidServiceCode(body.tjenesteKode, environment)) return@post

        val content = getContentMessage(body)
        var notifications: NotificationBEList? = null
        if (body.varsel != null) {
            notifications = getNotification(body.varsel)
        }

        val meldingResponse = altinnCorrespondenceService.insertCorrespondence(body.tjenesteKode, body.utgaveKode,
            body.orgnr, content, notifications = notifications)
        if (meldingResponse.status != "OK") {
            call.respond(HttpStatusCode.BadRequest, AnError(meldingResponse.message))
            return@post
        }
        call.respond(HttpStatusCode.OK, meldingResponse.message)
    }

fun getNotification(varsel: List<Varsel>): NotificationBEList? {
    val notificationBEList = NotificationBEList()
    varsel.forEach { varsel ->
        val notification = Notification()
        notification.textTokens = TextTokenSubstitutionBEList()
        notification.fromAddress = varsel.fraAdresse
        notification.languageCode = "1044"
        notification.shipmentDateTime = toXmlGregorianCalendar(varsel.forsendelseDatoTid)
        notification.notificationType = varsel.varselType.toString()
        val tittelToken = TextToken()
        tittelToken.tokenNum = 0
        tittelToken.tokenValue = if (varsel.varselType == VarselType.TokenTextOnly) varsel.tittel else varsel.melding
        notification.textTokens.textToken.add(tittelToken)
        val meldingToken = TextToken()
        meldingToken.tokenNum = 1
        meldingToken.tokenValue = if (varsel.varselType == VarselType.TokenTextOnly) varsel.melding else ""
        notification.textTokens.textToken.add(meldingToken)
        if (varsel.ekstraMottakere != null) {
            notification.receiverEndPoints = ReceiverEndPointBEList()
            varsel.ekstraMottakere.forEach {
                val receiverEndPoint = ReceiverEndPoint()
                receiverEndPoint.receiverAddress = it.mottakerAdresse
                receiverEndPoint.transportType = TransportType.fromValue(it.forsendelseType.toString())
                notification.receiverEndPoints.receiverEndPoint.add(receiverEndPoint)
            }
        }
        notificationBEList.notification.add(notification)
    }

    return notificationBEList
}

// @Group(GROUP_NAME)
// @Location("$API_V1/altinn/meldinger/vedlegg")
// data class NyttVedlegg(val `in`: String = "formData", val name: String, val type: File, val description: String)
// class MultiPartFormDataContent

// fun Routing.postFile(altinnCorrespondenceService: AltinnCorrespondenceService, environment: Environment) =
//    post<NyttVedlegg, NoBody>("Last opp vedlegg".responds(ok<CorrespondenceResponse>(), serviceUnavailable<AnError>(), badRequest<AnError>())) {
//        param, _ ->
//
//        val multipart = call.receiveMultipart()
//        multipart.forEachPart { part ->
//            when (part) {
//                is PartData.FormItem -> {
//                    if (part.name == "title") {
//                        title = part.value
//                    }
//                }
//                is PartData.FileItem -> {
//                    val ext = File(part.originalFileName).extension
//                    val file = File(uploadDir, "upload-${System.currentTimeMillis()}-${session.userId.hashCode()}-${title.hashCode()}.$ext")
//                    part.streamProvider().use { input -> file.outputStream().buffered().use { output -> input.copyToSuspend(output) } }
//                    videoFile = file
//                }
//            }
//
//            part.dispose()
//        }
//        call.respond(mapOf("status" to true))
//    }

//    { param ->
// ("Last opp et vedlegg".responds(ok<CorrespondenceResponse>(),
// serviceUnavailable<AnError>(), badRequest<AnError>(), unAuthorized<Unit>()))
//        logger.info { "Upload a file to service" }
//        if (!call.request.isFormMultipart()) {
//            logger.error { "Not contenttype form-mulitpart" }
//            return@post
//        }
//        val currentUser = call.principal<UserIdPrincipal>()!!.name
//
//        val approvedUsers = environment.application.users.split(",")
//        val userExist = approvedUsers.contains(currentUser)
//        if (!userExist) {
//            val msg = "Autentisert bruker $currentUser eksisterer ikke i listen for godkjente brukere."
//            application.environment.log.warn(msg)
//            call.respond(HttpStatusCode.ServiceUnavailable, AnError(msg))
//            return@post
//        }

//        if (notValidServiceCode(body.tjenesteKode, environment)) return@post

//        val content = getContentMessage(body)
//
//        val meldingResponse = altinnCorrespondenceService.insertCorrespondence(body.tjenesteKode, body.utgaveKode, body.orgnr, content)
//        if (meldingResponse.status != "OK") {
//            call.respond(HttpStatusCode.BadRequest, AnError(meldingResponse.message))
//            return@post
//        }
//        call.respond(HttpStatusCode.OK, meldingResponse.message)
fun getContentMessage(body: PostCorrespondenceBody): ExternalContentV2 {
    val contentV2 = ExternalContentV2()
    contentV2.languageCode = "1044"
    contentV2.messageTitle = body.melding.tittel
    contentV2.messageBody = body.melding.innhold
    contentV2.messageSummary = body.melding.sammendrag
    contentV2.customMessageData = body.melding.tjenesteAttributter
    if (body.vedlegger != null) {
        addAttachments(contentV2, body)
    }
    return contentV2
}

private fun addAttachments(contentV2: ExternalContentV2, body: PostCorrespondenceBody) {
    contentV2.attachments = AttachmentsV2()
    contentV2.attachments.binaryAttachments = BinaryAttachmentExternalBEV2List()
    body.vedlegger?.forEach { vedlegg ->
        val attachmentV2 = BinaryAttachmentV2()
        attachmentV2.fileName = vedlegg.filnavn
        attachmentV2.name = vedlegg.navn
        attachmentV2.data = decodeBase64(vedlegg.data)
        attachmentV2.functionType = AttachmentFunctionType.UNSPECIFIED
        contentV2.attachments.binaryAttachments.binaryAttachmentV2.add(attachmentV2)
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

private fun ApplicationRequest.isFormMultipart(): Boolean {
    return contentType().withoutParameters().match(ContentType.MultiPart.FormData)
}