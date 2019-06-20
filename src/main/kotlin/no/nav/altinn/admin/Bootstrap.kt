package no.nav.altinn.admin

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.basic
import io.ktor.features.*
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.JacksonConverter
import io.ktor.locations.Locations
import io.ktor.request.path
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.error
import io.prometheus.client.hotspot.DefaultExports
import mu.KotlinLogging
import no.nav.altinn.admin.api.nais
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.Contact
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.Information
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.Swagger
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.SwaggerUi
import no.nav.altinn.admin.common.*
import no.nav.altinn.admin.ldap.LDAPAuthenticate
import no.nav.altinn.admin.service.srr.AltinnSRRService
import no.nav.altinn.admin.service.srr.ssrAPI
import no.nav.altinn.admin.ws.*
import org.slf4j.event.Level
import java.util.concurrent.TimeUnit

const val AUTHENTICATION_BASIC = "basicAuth"

val swagger = Swagger(
        info = Information(
                version = System.getenv("APP_VERSION")?.toString() ?: "",
                title = "Altinn Admin API",
                description = "[altinn-admin](https://github.com/navikt/altinn-admin)",
                contact = Contact(
                        name = "Mona Terning, Ole-Petter Pettersen, Hans Arild Runde, Richard Oseng",
                        url = "https://github.com/navikt/altinn-admin",
                        email = "nav.altinn.lokalforvaltning@nav.no")
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
    logger.info { "Starting server" }
    System.setProperty("javax.xml.soap.SAAJMetaFactory", "com.sun.xml.messaging.saaj.soap.SAAJMetaFactoryImpl")

    install(DefaultHeaders)
    install(ConditionalHeaders)
    install(Compression)
    install(AutoHeadResponse)
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith(API_V1) }
    }
    // install(XForwardedHeadersSupport) - is this needed, and supported in reverse proxy in matter?
    install(StatusPages) {
        exception<Throwable> { cause ->
            logger.error(cause)
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
    install(Authentication) {
        basic(name = AUTHENTICATION_BASIC) {
            realm = "altinn-admin"
            validate { credentials ->
                LDAPAuthenticate(environment.application).use { ldap ->
                    if (ldap.canUserAuthenticate(credentials.name, credentials.password))
                        UserIdPrincipal(credentials.name)
                    else
                        null
                }
            }
        }
    }
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(objectMapper))
    }
    install(Locations)
    install(CallId) {
        generate { randomUuid() }
        verify { callId: String -> callId.isNotEmpty() }
        header(HttpHeaders.XCorrelationId)
    }

    val swaggerUI = SwaggerUi()

    val stsClient by lazy {
        logger.debug { "STS ${environment.stsUrl} service username : ${environment.application.username}" }
        stsClient(
                stsUrl = environment.stsUrl,
                credentials = environment.application.username to environment.application.password
        )
    }

    logger.info { "Installing routes" }
    install(Routing) {
        get("/") { call.respondRedirect(SWAGGER_URL_V1) }
        get("/api") { call.respondRedirect(SWAGGER_URL_V1) }
        get(API_V1) { call.respondRedirect(SWAGGER_URL_V1) }
        get("$API_V1/apidocs") { call.respondRedirect(SWAGGER_URL_V1) }
        get("$API_V1/apidocs/{fileName}") {
            val fileName = call.parameters["fileName"]
            if (fileName == "swagger.json") call.respond(swagger) else swaggerUI.serve(fileName, call)
        }

        logger.info { "Installing altinn srr api" }
        ssrAPI(altinnSrrService = AltinnSRRService(environment) {
            Clients.iRegisterSRRAgencyExternalBasic(environment.altinn.altinnAdminUrl).apply {
                logger.debug { "Using devProfile : ${environment.application.devProfile}" }
                when (environment.application.devProfile) {
                    true -> stsClient.configureFor(this, STS_SAML_POLICY_NO_TRANSPORT_BINDING)
                    false -> stsClient.configureFor(this)
                }
            }
        }, environment = environment)
        nais(environment, readinessCheck = { applicationState.initialized }, livenessCheck = { applicationState.running })
    }
}
