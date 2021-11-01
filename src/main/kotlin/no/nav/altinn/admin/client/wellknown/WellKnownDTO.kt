package no.nav.altinn.admin.client.wellknown

data class WellKnownDTO(
    val authorization_endpoint: String,
    val issuer: String,
    val jwks_uri: String,
    val token_endpoint: String,
)

fun WellKnownDTO.toWellKnown() = WellKnown(
    issuer = this.issuer,
    jwksUri = this.jwks_uri,
)

data class JwtIssuer(
    val acceptedAudienceList: List<String>,
    val jwtIssuerType: JwtIssuerType,
    val wellKnown: WellKnown,
)

enum class JwtIssuerType {
    INTERNAL_AZUREAD,
}
