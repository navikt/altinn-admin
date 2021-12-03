package no.nav.altinn.admin.client.wellknown

import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking
import no.nav.altinn.admin.client.httpClientProxy

fun getWellKnown(wellKnownUrl: String) =
    runBlocking { httpClientProxy().use { client -> client.get<WellKnown>(wellKnownUrl) } }

data class WellKnown(
    val authorization_endpoint: String,
    val token_endpoint: String,
    val jwks_uri: String,
    val issuer: String
)
