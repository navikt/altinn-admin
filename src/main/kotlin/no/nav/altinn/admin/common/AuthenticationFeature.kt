package no.nav.altinn.admin.common

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.jwt.JWTCredential
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import java.net.URL
import java.util.concurrent.TimeUnit
import mu.KotlinLogging
import net.logstash.logback.argument.StructuredArguments
import no.nav.altinn.admin.client.wellknown.JwtIssuer

private val logger = KotlinLogging.logger { }

fun Application.installJwtAuthentication(
    jwtIssuerList: List<JwtIssuer>,
) {
    install(Authentication) {
        jwtIssuerList.forEach { jwtIssuer ->
            configureJwt(
                jwtIssuer = jwtIssuer,
            )
        }
    }
}

fun Authentication.Configuration.configureJwt(
    jwtIssuer: JwtIssuer,
) {
    val jwkProviderSelvbetjening = JwkProviderBuilder(URL(jwtIssuer.wellKnown.jwksUri))
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()
    jwt(name = jwtIssuer.jwtIssuerType.name) {
        verifier(
            jwkProvider = jwkProviderSelvbetjening,
            issuer = jwtIssuer.wellKnown.issuer,
        )
        validate { credential ->
            val credentialsHasExpectedAudience = hasExpectedAudience(
                credentials = credential,
                expectedAudience = jwtIssuer.acceptedAudienceList,
            )
            if (credentialsHasExpectedAudience) {
                JWTPrincipal(credential.payload)
            } else {
                logger.warn(
                    "Auth: Unexpected audience for jwt {}, {}",
                    StructuredArguments.keyValue("issuer", credential.payload.issuer),
                    StructuredArguments.keyValue("audience", credential.payload.audience),
                )
                null
            }
        }
    }
}

fun hasExpectedAudience(
    credentials: JWTCredential,
    expectedAudience: List<String>,
): Boolean {
    return expectedAudience.any {
        credentials.payload.audience.contains(it)
    }
}
