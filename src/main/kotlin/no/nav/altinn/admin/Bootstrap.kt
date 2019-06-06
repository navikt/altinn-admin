package no.nav.altinn.admin

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.basic
import io.ktor.client.utils.CacheControl
import io.ktor.features.*
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.jackson.JacksonConverter
import io.ktor.locations.Locations
import io.ktor.request.path
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.prometheus.client.hotspot.DefaultExports
import mu.KotlinLogging
import no.nav.altinn.admin.api.*
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.Contact
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.Information
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.Swagger
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.SwaggerUi
import no.nav.altinn.admin.common.*
import no.nav.altinn.admin.ldap.LDAPAuthenticate
import no.nav.altinn.admin.service.srr.AltinnSRRService
import no.nav.altinn.admin.ws.*
import org.slf4j.event.Level
import java.util.concurrent.TimeUnit

const val AUTHENTICATION_BASIC = "basicAuth"

val swagger = Swagger(
        info = Information(
                version = System.getenv("APP_VERSION")?.toString() ?: "",
                title = "Altinn Admin API",
                description = "[altinn-admin](https://github.com/navikt/TODO)",
                contact = Contact(
                        name = "Mona, Ole-Petter, Hans Arild, Richard",
                        url = "https://github.com/navikt/TODO",
                        email = "")
        )
)

private val logger = KotlinLogging.logger { }

// internal const val JAAS_PLAIN_LOGIN = "org.apache.kafka.common.security.plain.PlainLoginModule"
// internal const val JAAS_REQUIRED = "required"
internal const val SWAGGER_URL_V1 = "$API_V1/apidocs/index.html?url=swagger.json"

fun main() = bootstrap(ApplicationState(), Environment())

fun bootstrap(applicationState: ApplicationState, environment: Environment) {

    val applicationServer = embeddedServer(
        Netty, environment.application.port, module = { mainModule(environment, applicationState) }
    )
    applicationState.initialized = true
    logger.info { "Application ready" }

    DefaultExports.initialize()
    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info { "Shutdown hook called, shutting down gracefully" }
        applicationState.initialized = false
        applicationState.running = false
        applicationServer.stop(1, 5, TimeUnit.SECONDS)
    })
    applicationServer.start(wait = true)
}

fun Application.mainModule(environment: Environment, applicationState: ApplicationState) {
    /*val jwkProvider = JwkProviderBuilder(URL(environment.jwt.jwksUri))
            .cached(10, 24, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build()

     */

    install(StatusPages) {
        notFoundHandler()
        exceptionHandler()
    }
    install(CallLogging) {
        level = Level.DEBUG
        filter { call -> call.request.path().startsWith(API_V1) }
        callIdMdc(MDC_CALL_ID)
    }
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(objectMapper))
    }

    /*
    install(Authentication) {
        jwt {
            skipWhen { environment.application.devProfile }
            realm = "altinn-admin"
            verifier(jwkProvider, environment.jwt.issuer)
            validate { credentials ->
                logger.info { "Auth: User requested resource '${request.url()}'" }
                try {
                    requireNotNull(credentials.payload.audience) { "Auth: Missing audience in token" }
                    require(credentials.payload.audience.contains(environment.jwt.audience)) { "Auth: Valid audience not found in claims" }
                    logger.info {
                        "Auth: Resource requested by '${credentials.payload.getClaim("name").asString()}' " +
                                "\n NAV ident: '${credentials.payload.getClaim("NAVident").asString()}'" +
                                "\n Unique Name: '${credentials.payload.getClaim("unique_name").asString()}'" +
                                "\n IP address: '${credentials.payload.getClaim("ipaddr").asString()}'" +
                                "\n Groups: '${credentials.payload.getClaim("groups").asArray(String::class.java).joinToString()}'"
                    }
                    logger.debug { "Auth: Claims validated, user is authorized to request this resource" }
                    JWTPrincipal(credentials.payload)
                } catch (e: Throwable) {
                    logger.error(e) { "Auth: Token validation failed: ${e.message}" }
                    null
                }
            }
        }
    }

     */

    install(DefaultHeaders) {
        header(HttpHeaders.CacheControl, CacheControl.NO_CACHE)
    }
    install(AutoHeadResponse)
    install(ConditionalHeaders)
    install(Compression)

    install(CallId) {
        generate { randomUuid() }
        verify { callId: String -> callId.isNotEmpty() }
        header(HttpHeaders.XCorrelationId)
    }

    install(Authentication) {
        basic(name = AUTHENTICATION_BASIC) {
            realm = "altinn-admin"
            validate { credentials ->
                LDAPAuthenticate(environment).use { ldap ->
                    if (ldap.canUserAuthenticate(credentials.name, credentials.password))
                        UserIdPrincipal(credentials.name)
                    else
                        null
                }
            }
        }
    }

    install(Locations)

    val swaggerUI = SwaggerUi()

    val stsClient by lazy {
        stsClient(
                stsUrl = environment.stsUrl,
                credentials = environment.application.username to environment.application.password
        )
    }

    logger.info { "Installing routes" }
    install(Routing) {

        get("/") { call.respondRedirect(SWAGGER_URL_V1) }
        get("/api") { call.respondRedirect(SWAGGER_URL_V1) }
        get("$API_V1") { call.respondRedirect(SWAGGER_URL_V1) }
        get("$API_V1/apidocs") { call.respondRedirect(SWAGGER_URL_V1) }
        get("$API_V1/apidocs/{fileName}") {
            val fileName = call.parameters["fileName"]
            if (fileName == "swagger.json") call.respond(swagger) else swaggerUI.serve(fileName, call)
        }

        api(altinnSrrService = AltinnSRRService(environment) {
            Clients.iRegisterSRRAgencyExternalBasic(environment.altinn.altinnAdminUrl).apply {
                when (environment.application.devProfile) {
                    true -> stsClient.configureFor(this, STS_SAML_POLICY_NO_TRANSPORT_BINDING)
                    false -> stsClient.configureFor(this)
                }
            }
        })
        nais(readinessCheck = { applicationState.initialized }, livenessCheck = { applicationState.running })
    }
}
