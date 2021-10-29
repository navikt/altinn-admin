package no.nav.altinn.admin.client.azuread

import java.io.Serializable
import java.time.LocalDateTime

data class AzureAdToken(
    val accessToken: String,
    val expires: LocalDateTime,
) : Serializable
