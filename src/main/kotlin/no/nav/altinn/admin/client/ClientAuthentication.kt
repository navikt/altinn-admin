package no.nav.altinn.admin.client

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.time.Instant
import java.util.Date
import java.util.UUID
import mu.KotlinLogging
import no.nav.altinn.admin.Environment

private val logger = KotlinLogging.logger { }

class ClientAuthentication(
    private val maskinporten: Environment.Maskinporten
) {
    private val rsaKey = maskinporten.clientJwk.parseToRsaKey()

    fun clientAssertion(): String {
        return clientAssertion(
            maskinporten.clientId,
            maskinporten.baseUrl,
            maskinporten.scopes,
            rsaKey
        ).also {
            logger.debug {
                "JWK with keyID: ${rsaKey.keyID} used to sign generated JWT for integration with: ${maskinporten.baseUrl}"
            }
        }
    }
}

internal fun clientAssertion(clientId: String, audience: String, scopes: String, rsaKey: RSAKey): String {
    val now = Date.from(Instant.now())
    return JWTClaimsSet.Builder()
        .issuer(clientId)
        .audience(audience)
        .issueTime(now)
        .expirationTime(Date.from(Instant.now().plusSeconds(120)))
        .jwtID(UUID.randomUUID().toString())
        .claim("scope", scopes)
        .build()
        .sign(rsaKey)
        .serialize()
}

internal fun String.parseToRsaKey(): RSAKey = RSAKey.parse(this)

internal fun JWTClaimsSet.sign(rsaKey: RSAKey): SignedJWT =
    SignedJWT(
        JWSHeader.Builder(JWSAlgorithm.RS256)
            .keyID(rsaKey.keyID)
            .type(JOSEObjectType.JWT).build(),
        this
    ).apply {
        sign(RSASSASigner(rsaKey.toPrivateKey()))
    }
