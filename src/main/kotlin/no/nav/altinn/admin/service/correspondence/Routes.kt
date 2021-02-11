package no.nav.altinn.admin.service.correspondence

import io.ktor.application.ApplicationCall
import io.ktor.application.application
import io.ktor.application.call
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.principal
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.locations.KtorExperimentalLocationsAPI
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
import no.nav.altinn.admin.common.API_V2
import no.nav.altinn.admin.common.dateTimeToXmlGregorianCalendar
import no.nav.altinn.admin.common.dateToXmlGregorianCalendar
import no.nav.altinn.admin.common.decodeBase64
import no.nav.altinn.admin.common.isDate
import no.nav.altinn.admin.common.isDateTime
import no.nav.altinn.admin.common.randomUuid
import no.nav.altinn.admin.common.toXmlGregorianCalendar

@KtorExperimentalLocationsAPI
fun Routing.correspondenceAPI(altinnCorrespondenceService: AltinnCorrespondenceService, environment: Environment) {
    getCorrespondence(altinnCorrespondenceService, environment)
    getCorrespondenceFiltered(altinnCorrespondenceService, environment)
    getCorrespondenceFiltered2(altinnCorrespondenceService, environment)
    getCorrespondence2(altinnCorrespondenceService, environment)
    getCorrespondenceFiltered3(altinnCorrespondenceService, environment)
    getCorrespondenceFiltered4(altinnCorrespondenceService, environment)
    postCorrespondence(altinnCorrespondenceService, environment)
//    postFile(altinnCorrespondenceService, environment)
}

internal data class AnError(val error: String)

internal const val GROUP_NAME = "Correspondence"

private val logger = KotlinLogging.logger { }

@KtorExperimentalLocationsAPI
@Group(GROUP_NAME)
@Location("$API_V1/altinn/meldinger/hent/{tjeneste}/{fraDato}/{tilDato}/{mottaker}")
data class MeldingsFilter(val tjeneste: CorrespondenceType, val fraDato: String, val tilDato: String, val mottaker: String)

@KtorExperimentalLocationsAPI
fun Routing.getCorrespondenceFiltered(altinnCorrespondenceService: AltinnCorrespondenceService, environment: Environment) =
    get<MeldingsFilter>(
        "Hent status på filtrerte meldinger fra en meldingstjeneste".securityAndReponds(
            BasicAuthSecurity(), ok<CorrespondenceDetails>(),
            serviceUnavailable<AnError>(), badRequest<AnError>()
        )
    ) { param ->

        if (notValidServiceCode(param.tjeneste.servicecode, environment)) return@get

        if (param.mottaker.isNullOrEmpty() || param.mottaker.length < 9) {
            call.respond(HttpStatusCode.BadRequest, AnError("Mottaker id is empty or wrong"))
            return@get
        }

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
            val fraDato = dateToXmlGregorianCalendar(fromDate)
            val tilDato = dateToXmlGregorianCalendar(toDate)
            val correspondenceResponse = altinnCorrespondenceService.getCorrespondenceDetails(param.tjeneste.servicecode, 1, fraDato, tilDato, param.mottaker)

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

@KtorExperimentalLocationsAPI
@Group(GROUP_NAME)
@Location("$API_V2/altinn/meldinger/hent/{tjenesteKode}/{utgaveKode}/{fraDato}/{tilDato}/{mottaker}")
data class MeldingsFilter3(val tjenesteKode: String, val utgaveKode: String, val fraDato: String, val tilDato: String, val mottaker: String)

@KtorExperimentalLocationsAPI
fun Routing.getCorrespondenceFiltered3(altinnCorrespondenceService: AltinnCorrespondenceService, environment: Environment) =
    get<MeldingsFilter3>(
        "Hent status på filtrerte meldinger fra en meldingstjeneste".securityAndReponds(
            BasicAuthSecurity(), ok<CorrespondenceDetails>(),
            serviceUnavailable<AnError>(), badRequest<AnError>()
        )
    ) { param ->

        val scList = filterOutServiceCode(environment, param.tjenesteKode)
        if (scList.size == 0) {
            call.respond(HttpStatusCode.BadRequest, AnError("Ugyldig tjeneste kode oppgitt"))
            return@get
        }
        val secList = scList.map { it.second }
        if (param.utgaveKode.trim().isEmpty() || param.utgaveKode.toIntOrNull() == null || !secList.contains(param.utgaveKode)) {
            call.respond(HttpStatusCode.BadRequest, AnError("Ugyldig utgave kode oppgitt"))
            return@get
        }

        if (param.mottaker.isNullOrEmpty() || param.mottaker.length < 9) {
            call.respond(HttpStatusCode.BadRequest, AnError("Mottaker id is empty or wrong"))
            return@get
        }

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
            val fraDato = dateToXmlGregorianCalendar(fromDate)
            val tilDato = dateToXmlGregorianCalendar(toDate)
            val uk = param.utgaveKode.toInt()
            val correspondenceResponse = altinnCorrespondenceService.getCorrespondenceDetails(param.tjenesteKode, uk, fraDato, tilDato, param.mottaker)

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

@KtorExperimentalLocationsAPI
@Group(GROUP_NAME)
@Location("$API_V1/altinn/meldinger/hent/{tjeneste}/{fraDato}/{tilDato}")
data class MeldingsFilter2(val tjeneste: CorrespondenceType, val fraDato: String, val tilDato: String)

@KtorExperimentalLocationsAPI
fun Routing.getCorrespondenceFiltered2(altinnCorrespondenceService: AltinnCorrespondenceService, environment: Environment) =
    get<MeldingsFilter2>(
        "Hent status på filtrerte meldinger fra en meldingstjeneste".securityAndReponds(
            BasicAuthSecurity(), ok<CorrespondenceDetails>(),
            serviceUnavailable<AnError>(), badRequest<AnError>()
        )
    ) { param ->

        if (notValidServiceCode(param.tjeneste.servicecode, environment)) return@get

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
            val fraDato = dateToXmlGregorianCalendar(fromDate)
            val tilDato = dateToXmlGregorianCalendar(toDate)
            val correspondenceResponse = altinnCorrespondenceService.getCorrespondenceDetails(param.tjeneste.servicecode, 1, fraDato, tilDato)

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

@KtorExperimentalLocationsAPI
@Group(GROUP_NAME)
@Location("$API_V2/altinn/meldinger/hent/{tjenesteKode}/{utgaveKode}/{fraDato}/{tilDato}")
data class MeldingsFilter4(val tjenesteKode: String, val utgaveKode: String, val fraDato: String, val tilDato: String)

@KtorExperimentalLocationsAPI
fun Routing.getCorrespondenceFiltered4(altinnCorrespondenceService: AltinnCorrespondenceService, environment: Environment) =
    get<MeldingsFilter4>(
        "Hent status på filtrerte meldinger fra en meldingstjeneste".securityAndReponds(
            BasicAuthSecurity(), ok<CorrespondenceDetails>(),
            serviceUnavailable<AnError>(), badRequest<AnError>()
        )
    ) { param ->

        val scList = filterOutServiceCode(environment, param.tjenesteKode)
        if (scList.size == 0) {
            call.respond(HttpStatusCode.BadRequest, AnError("Ugyldig tjeneste kode oppgitt"))
            return@get
        }
        val secList = scList.map { it.second }
        if (param.utgaveKode.trim().isEmpty() || param.utgaveKode.toIntOrNull() == null || !secList.contains(param.utgaveKode)) {
            call.respond(HttpStatusCode.BadRequest, AnError("Ugyldig utgave kode oppgitt"))
            return@get
        }

        val fromDate = param.fraDato
        val toDate = param.tilDato
        if (fromDate.isNullOrEmpty() || toDate.isNullOrEmpty()) {
            call.respond(HttpStatusCode.BadRequest, AnError("Fra eller til dato på filter er ikke oppgitt"))
            return@get
        }
        if (!(isDate(fromDate) || isDateTime(fromDate))) {
            call.respond(HttpStatusCode.BadRequest, AnError("Fra dato er oppgitt i feil format, bruk yyyy-mm-dd, eller yyyy-mm-ddThh:mm:ss"))
            return@get
        }
        if (!(isDate(toDate) || isDateTime(toDate))) {
            call.respond(HttpStatusCode.BadRequest, AnError("Til dato er oppgitt i feil format, bruk yyyy-mm-dd, eller yyyy-mm-ddThh:mm:ss"))
            return@get
        }
        try {
            val fraDato = if (isDate(fromDate)) dateToXmlGregorianCalendar(fromDate) else dateTimeToXmlGregorianCalendar(fromDate)
            val tilDato = if (isDate(toDate)) dateToXmlGregorianCalendar(toDate) else dateTimeToXmlGregorianCalendar(toDate)
            val uk = param.utgaveKode.toInt()
            val correspondenceResponse = altinnCorrespondenceService.getCorrespondenceDetails(param.tjenesteKode, uk, fraDato, tilDato)

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

@KtorExperimentalLocationsAPI
@Group(GROUP_NAME)
@Location("$API_V1/altinn/meldinger/hent/{tjeneste}")
data class TjenesteKode(val tjeneste: CorrespondenceType)

@KtorExperimentalLocationsAPI
fun Routing.getCorrespondence(altinnCorrespondenceService: AltinnCorrespondenceService, environment: Environment) =
    get<TjenesteKode>(
        "Hent status meldinger fra en meldingstjeneste".securityAndReponds(
            BasicAuthSecurity(), ok<CorrespondenceDetails>(),
            serviceUnavailable<AnError>(), badRequest<AnError>()
        )
    ) { param ->

        if (notValidServiceCode(param.tjeneste.servicecode, environment)) return@get
        try {
            val correspondenceResponse = altinnCorrespondenceService.getCorrespondenceDetails(param.tjeneste.servicecode, 1)

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

@KtorExperimentalLocationsAPI
@Group(GROUP_NAME)
@Location("$API_V2/altinn/meldinger/hent/{tjenesteKode}/{utgaveKode}")
data class TjenesteKode2(val tjenesteKode: String, val utgaveKode: String)

@KtorExperimentalLocationsAPI
fun Routing.getCorrespondence2(altinnCorrespondenceService: AltinnCorrespondenceService, environment: Environment) =
    get<TjenesteKode2>(
        "Hent status meldinger fra en meldingstjeneste".securityAndReponds(
            BasicAuthSecurity(), ok<CorrespondenceDetails>(),
            serviceUnavailable<AnError>(), badRequest<AnError>()
        )
    ) { param ->

        val scList = filterOutServiceCode(environment, param.tjenesteKode)
        if (scList.size == 0) {
            call.respond(HttpStatusCode.BadRequest, AnError("Ugyldig tjeneste kode oppgitt"))
            return@get
        }
        val secList = scList.map { it.second }
        if (param.utgaveKode.trim().isEmpty() || param.utgaveKode.toIntOrNull() == null || !secList.contains(param.utgaveKode)) {
            call.respond(HttpStatusCode.BadRequest, AnError("Ugyldig utgave kode oppgitt"))
            return@get
        }

        try {
            val uk = param.utgaveKode.toInt()
            val correspondenceResponse = altinnCorrespondenceService.getCorrespondenceDetails(param.tjenesteKode, uk)

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

@KtorExperimentalLocationsAPI
@Group(GROUP_NAME)
@Location("$API_V1/altinn/meldinger/send")
class SendMelding

@KtorExperimentalLocationsAPI
fun Routing.postCorrespondence(altinnCorrespondenceService: AltinnCorrespondenceService, environment: Environment) =
    post<SendMelding, PostCorrespondenceBody>(
        "Send melding til virksomhet".securityAndReponds(
            BasicAuthSecurity(), ok<CorrespondenceResponse>(),
            serviceUnavailable<AnError>(), badRequest<AnError>(), unAuthorized<Unit>()
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

        if (notValidServiceCode(body.tjeneste.servicecode, environment)) return@post

        if (!body.synligdato.isNullOrEmpty() && !isDate(body.synligdato)) {
            call.respond(HttpStatusCode.BadRequest, AnError("synligdato er i feil format, må være yyyy-mm-dd"))
            return@post
        }

        if (!body.tidsfrist.isNullOrEmpty() && !isDate(body.tidsfrist)) {
            call.respond(HttpStatusCode.BadRequest, AnError("tidsfrist er i feil format, må være yyyy-mm-dd"))
            return@post
        }
        val distinctVarseler = body.varsel?.distinctBy { it.tittel to it.melding to it.ekstraMottakere[0].forsendelseType to it.ekstraMottakere[0].mottakerAdresse }?.toList()
        if (distinctVarseler != null && distinctVarseler.size != body.varsel.size) {
            call.respond(HttpStatusCode.BadRequest, AnError("melding innholder lik varsel"))
            return@post
        }

        val content = getContentMessage(body)
        var notifications: NotificationBEList? = null
        if (body.varsel != null) {
            notifications = getNotification(body.varsel)
        }

        val synligDato = if (!body.synligdato.isNullOrEmpty()) dateToXmlGregorianCalendar(body.synligdato) else null
        val tidsfrist = if (!body.tidsfrist.isNullOrEmpty()) dateToXmlGregorianCalendar(body.tidsfrist) else null
        val sendersreferanse = if (body.sendersreferanse.isNullOrEmpty()) randomUuid() else body.sendersreferanse

        val meldingResponse = altinnCorrespondenceService.insertCorrespondence(
            body.tjeneste.servicecode, body.tjeneste.serviceeditioncode,
            body.orgnr, content, notifications = notifications, synligDato, tidsfrist, sendersreferanse
        )
        if (meldingResponse.status != "OK") {
            call.respond(HttpStatusCode.BadRequest, AnError(meldingResponse.message))
            return@post
        }
        call.respond(HttpStatusCode.OK, meldingResponse.message)
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

private fun filterOutServiceCode(environment: Environment, tjenesteKode: String): MutableList<Pair<String, String>> {
    val scSecList = environment.correspondeceService.serviceCodes.split(",")
    val scList = mutableListOf<Pair<String, String>>()
    scSecList.forEach {
        val sc = it.split(":")
        if (sc[0] == tjenesteKode)
            scList.add(Pair(sc[0], if (sc.size > 1) sc[1] else "1"))
    }
    return scList
}
