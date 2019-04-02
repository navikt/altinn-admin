package no.nav.altinn.admin.common

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.application.ApplicationCall
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.principal
import io.ktor.features.origin
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.request.ApplicationRequest
import io.ktor.request.receive
import io.ktor.request.receiveStream
import io.ktor.response.respondText
//import no.nav.dsop.kontroll.service.bank.models.Transactions
import java.text.SimpleDateFormat

internal fun ApplicationCall.getAuthenticatedUser(): AuthenticatedUser =
    principal<JWTPrincipal>()?.let { principal ->
        principal.payload.let {
            val firstName = it.getClaim("given_name").asString()
            val lastName = it.getClaim("family_name").asString()
            AuthenticatedUser(
                identifier = it.getClaim("NAVident").asString(),
                email = it.getClaim("unique_name").asString(),
                lastName = lastName,
                firstName = firstName
            )
        }
    } ?: AuthenticatedUser("null", "null", "null", "null")

internal suspend fun ApplicationCall.respondJson(provider: suspend () -> String) =
    respondText(ContentType.Application.Json, HttpStatusCode.OK, provider)

internal suspend fun ApplicationCall.receiveTextUtf8() =
    receiveStream().bufferedReader(Charsets.UTF_8).use { br -> br.readText() }

internal fun Long.getDateTimeFormatted(): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz").format(this)

//internal inline fun <reified T> String.deserializeJson(): T {
//    try {
//        return objectMapper.readValue(this)
//    } catch (e: Exception) {
//        when (T::class) {
//            Transactions::class ->
//                throw TransactionsParsingException("Could not parse transactions in returned response from bank API \n ${e.message}")
//            else -> when (e) {
//                is MissingKotlinParameterException, is MismatchedInputException, is JsonParseException ->
//                    throw InvalidInputException("Could not parse payload - invalid input (body: '$this'). \n${e.message}")
//                else -> throw e
//            }
//        }
//    }
//}

internal suspend inline fun <reified T : Any> ApplicationCall.receiveTry() = try {
    receive<T>()
} catch (e: Exception) {
    when (e) {
        is InvalidFormatException, is MissingKotlinParameterException, is JsonParseException, is MismatchedInputException -> {
            throw InvalidInputException(e.message ?: "")
        }
        else -> throw e
    }
}

internal fun ApplicationRequest.url(): String {
    val port = when (origin.port) {
        in listOf(80, 443) -> ""
        else -> ":${origin.port}"
    }
    return "${origin.scheme}://${origin.host}$port${origin.uri}"
}

val HttpHeaders.NavApiKey: String
    get() = "x-nav-apiKey"
val HttpHeaders.CorrelationId: String
    get() = "CorrelationID"
val HttpHeaders.CustomerId: String
    get() = "CustomerID"
val HttpHeaders.PartyId: String
    get() = "PartyID"
val HttpHeaders.LegalMandate: String
    get() = "Legal-Mandate"
