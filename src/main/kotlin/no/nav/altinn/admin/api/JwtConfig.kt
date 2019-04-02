package no.nav.altinn.admin.api

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import io.ktor.auth.Principal
import io.ktor.auth.jwt.JWTCredential
import io.ktor.auth.jwt.JWTPrincipal
import mu.KotlinLogging
import no.nav.altinn.admin.Environment
import java.net.URL
import java.util.concurrent.TimeUnit

class JwtConfig(private val env: Environment) {

    val jwkProvider: JwkProvider = JwkProviderBuilder(URL(env.jwt.jwksUri))
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    fun validate(credentials: JWTCredential): Principal? {
        return try {
            requireNotNull(credentials.payload.audience) { "Auth: Missing audience in token" }
            require(credentials.payload.audience.contains(env.jwt.audience)) { "Auth: Valid audience not found in claims" }
            requireNotNull(credentials.payload.issuer) { "Auth: Missing issuer in token" }
            require(credentials.payload.issuer == env.jwt.issuer) { "Auth: Valid issuer not found in claims" }
            logger.info {
                "Auth: Resource requested by '${credentials.payload.getClaim("name").asString()}' " +
                    "\n NAV ident: '${credentials.payload.getClaim("NAVident").asString()}'" +
                    "\n Unique Name: '${credentials.payload.getClaim("unique_name").asString()}'" +
                    "\n IP address: '${credentials.payload.getClaim("ipaddr").asString()}'" +
                    "\n Groups: '${credentials.payload.getClaim("groups").asArray(String::class.java).joinToString()}'"
            }
            logger.debug { "Auth: Claims validated, user is authorized to request this resource" }
            JWTPrincipal(credentials.payload)
        } catch (e: Exception) {
            logger.error(e) { e.message }
            null
        }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
        const val REALM = "altinn admin realm"
    }
}
