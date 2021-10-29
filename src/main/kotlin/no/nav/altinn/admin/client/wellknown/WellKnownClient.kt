package no.nav.altinn.admin.client.wellknown

import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking
import no.nav.altinn.admin.client.httpClientProxy

fun getWellKnown(
    wellKnownUrl: String,
): WellKnown = runBlocking {
    httpClientProxy().use { client ->
        client.get<WellKnownDTO>(wellKnownUrl).toWellKnown()
    }
}
