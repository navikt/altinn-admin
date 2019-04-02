package no.nav.altinn.admin.api

import com.fasterxml.jackson.annotation.JsonInclude
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import mu.KotlinLogging
import no.nav.altinn.admin.common.CorrelationId
import no.nav.altinn.admin.common.InvalidInputException
import no.nav.altinn.admin.common.NotFoundException
import no.nav.altinn.admin.common.ServiceUnavailableException
import no.nav.altinn.admin.common.TooManyRequestsException
import no.nav.altinn.admin.common.TransactionsParsingException
import no.nav.altinn.admin.common.getCorrelationId
import no.nav.altinn.admin.common.url
import java.time.DateTimeException

private val logger = KotlinLogging.logger { }

fun StatusPages.Configuration.exceptionHandler() {
    exception<Throwable> { cause ->
        call.logErrorAndRespond(cause) { "An internal error occurred during routing" }
    }
    exception<InvalidInputException> { cause ->
        call.logErrorAndRespond(cause, HttpStatusCode.BadRequest) {
            "The request was either invalid or lacked required parameters"
        }
    }
    exception<DateTimeException> { cause ->
        call.logErrorAndRespond(cause, HttpStatusCode.BadRequest) {
            "Invalid date(s) in request parameters. Must be of format 'YYYY-MM-DD'"
        }
    }
    exception<NotFoundException> { cause ->
        call.logErrorAndRespond(cause, HttpStatusCode.NotFound) { "The requested resource was not found" }
    }
    exception<TooManyRequestsException> { cause ->
        call.logErrorAndRespond(cause, HttpStatusCode.TooManyRequests) { "Too many requests" }
    }
    exception<ServiceUnavailableException> { cause ->
        call.logErrorAndRespond(cause, HttpStatusCode.ServiceUnavailable) { "External service provider is unavailable" }
    }
    exception<TransactionsParsingException> { cause ->
        call.logErrorAndRespond(cause) { "Could not parse transaction response from bank API" }
    }
}

fun StatusPages.Configuration.notFoundHandler() {
    status(HttpStatusCode.NotFound) {
        call.respond(
            HttpStatusCode.NotFound,
            HttpErrorResponse(
                message = "The page or operation requested does not exist.",
                code = HttpStatusCode.NotFound, url = call.request.url()
            )
        )
    }
}

private suspend inline fun ApplicationCall.logErrorAndRespond(
    cause: Throwable,
    status: HttpStatusCode = HttpStatusCode.InternalServerError,
    lazyMessage: () -> String
) {
    val message = lazyMessage()
    logger.error(cause) { message }
    val response = HttpErrorResponse(
        url = this.request.url(),
        cause = cause.toString(),
        message = message,
        code = status,
        callId = getCorrelationId()
    )
    logger.error { "Status Page Response: $response" }
    this.respond(status, response)
}

@JsonInclude(JsonInclude.Include.NON_NULL)
internal data class HttpErrorResponse(
    val url: String,
    val message: String? = null,
    val cause: String? = null,
    val code: HttpStatusCode = HttpStatusCode.InternalServerError,
    val callId: CorrelationId? = null
)
