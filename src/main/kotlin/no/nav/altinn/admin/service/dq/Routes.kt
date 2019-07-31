package no.nav.altinn.admin.service.dq

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.locations.Location
import io.ktor.response.respond
import io.ktor.routing.Routing
import mu.KotlinLogging
import no.nav.altinn.admin.Environment
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.BasicAuthSecurity
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.Group
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.badRequest
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.get
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.ok
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.securityAndReponds
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.serviceUnavailable
import no.nav.altinn.admin.common.API_V1

fun Routing.dqAPI(altinnDqService: AltinnDQService, environment: Environment) {
    getARmessage(altinnDqService, environment)
}

internal data class AnError(val error: String)
internal const val GROUP_NAME = "DownloadQueue"

private val logger = KotlinLogging.logger { }

@Group(GROUP_NAME)
@Location("$API_V1/altinn/dq/hent/{arNummer}")
data class ArkivReferanse(val arNummer: String)

fun Routing.getARmessage(altinnDqService: AltinnDQService, environment: Environment) =
    get<ArkivReferanse>("hent AR melding fra dq".securityAndReponds(BasicAuthSecurity(),
        ok<DqResponse>(), serviceUnavailable<AnError>(), badRequest<AnError>())) {
        param ->

        if (param.arNummer.isNullOrEmpty()) {
            call.respond(HttpStatusCode.BadRequest, AnError("Ugyldig AR nummer oppgitt"))
            return@get
        }

        try {
            val dqResponse = altinnDqService.getMessageFromDq(param.arNummer)
            if (dqResponse.status == "Ok")
                call.respond(dqResponse)
            else
                call.respond(HttpStatusCode.NotFound, dqResponse.message)
        } catch (ee: Exception) {
            logger.error {
                "IDownloadQueueExternalBasic.GetArchivedFormTaskBasicDQ feilet  \n" +
                    "\n ErrorMessage  ${ee.message}" +
                    "\n LocalizedErrorMessage  ${ee.localizedMessage}"
            }
            call.respond(HttpStatusCode.InternalServerError, AnError("IDownloadQueueExternalBasic.GetArchivedFormTaskBasicDQ feilet"))
        }
    }
