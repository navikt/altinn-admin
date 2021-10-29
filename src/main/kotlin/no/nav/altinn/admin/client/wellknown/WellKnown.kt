package no.nav.altinn.admin.client.wellknown

data class WellKnown(
    val issuer: String,
    val jwksUri: String,
)
