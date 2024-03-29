package no.nav.altinn.admin

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.basic
import io.ktor.client.utils.CacheControl
import io.ktor.features.AutoHeadResponse
import io.ktor.features.CallId
import io.ktor.features.CallLogging
import io.ktor.features.Compression
import io.ktor.features.ConditionalHeaders
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.features.StatusPages
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.JacksonConverter
import io.ktor.locations.KtorExperimentalLocationsAPI
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
import java.util.concurrent.Executors
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.slf4j.MDCContext
import mu.KotlinLogging
import no.nav.altinn.admin.api.nais
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.Contact
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.Information
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.Swagger
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.SwaggerUi
import no.nav.altinn.admin.common.API_V1
import no.nav.altinn.admin.common.API_V2
import no.nav.altinn.admin.common.ApplicationState
import no.nav.altinn.admin.common.objectMapper
import no.nav.altinn.admin.common.randomUuid
import no.nav.altinn.admin.ldap.LDAPAuthenticate
import no.nav.altinn.admin.service.alerts.ExpireAlerts
import no.nav.altinn.admin.service.correspondence.AltinnCorrespondenceService
import no.nav.altinn.admin.service.correspondence.correspondenceAPI
import no.nav.altinn.admin.service.dq.AltinnDQService
import no.nav.altinn.admin.service.dq.dqAPI
import no.nav.altinn.admin.service.prefill.AltinnPrefillService
import no.nav.altinn.admin.service.prefill.prefillAPI
import no.nav.altinn.admin.service.receipt.AltinnReceiptService
import no.nav.altinn.admin.service.receipt.receiptsAPI
import no.nav.altinn.admin.service.srr.AltinnSRRService
import no.nav.altinn.admin.service.srr.ssrAPI
import no.nav.altinn.admin.ws.Clients
import no.nav.altinn.admin.ws.STS_SAML_POLICY_NO_TRANSPORT_BINDING
import no.nav.altinn.admin.ws.configureFor
import no.nav.altinn.admin.ws.stsClient
import org.slf4j.event.Level

const val AUTHENTICATION_BASIC = "basicAuth"
private val backgroundTasksContext = Executors.newFixedThreadPool(4).asCoroutineDispatcher() + MDCContext()

val swagger = Swagger(
    info = Information(
        version = System.getenv("APP_VERSION")?.toString() ?: "",
        title = "Altinn Admin API",
        description = "[altinn-admin](https://github.com/navikt/altinn-admin)",
        contact = Contact(
            name = "Slack #Team_Altinn",
            url = "https://github.com/navikt/altinn-admin",
            email = "nav.altinn.lokalforvaltning@nav.no"
        )
    )
)

private val logger = KotlinLogging.logger { }

internal const val SWAGGER_URL_V1 = "$API_V1/apidocs/index.html?url=swagger.json"

@KtorExperimentalLocationsAPI
fun main() = bootstrap(ApplicationState(), Environment())

@KtorExperimentalLocationsAPI
fun bootstrap(applicationState: ApplicationState, environment: Environment) {
    val applicationServer = embeddedServer(
        Netty, environment.application.port, module = { mainModule(environment, applicationState) }
    )
    applicationState.initialized = true
    logger.info { "Application ready" }

    DefaultExports.initialize()
    Runtime.getRuntime().addShutdownHook(
        Thread {
            logger.info { "Shutdown hook called, shutting down gracefully" }
            applicationState.initialized = false
            applicationState.running = false
            applicationServer.stop(1000, 5000)
        }
    )
    applicationServer.start(wait = true)
}

@KtorExperimentalLocationsAPI
fun Application.mainModule(environment: Environment, applicationState: ApplicationState) {
    logger.info { "Starting server" }
    System.setProperty("javax.xml.soap.SAAJMetaFactory", "com.sun.xml.messaging.saaj.soap.SAAJMetaFactoryImpl")

    install(DefaultHeaders) {
        header(HttpHeaders.CacheControl, CacheControl.NO_CACHE)
    }
    install(ConditionalHeaders)
    install(Compression)
    install(AutoHeadResponse)
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith(API_V1) }
        filter { call -> call.request.path().startsWith(API_V2) }
    }
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
        register(ContentType.Application.Xml, JacksonConverter(objectMapper))
    }
    install(Locations)
    install(CallId) {
        generate { randomUuid() }
        verify { callId: String -> callId.isNotEmpty() }
        header(HttpHeaders.XCorrelationId)
    }

    val swaggerUI = SwaggerUi()

    val stsClient by lazy {
        stsClient(
            stsUrl = environment.stsUrl,
            credentials = environment.application.username to environment.application.password
        )
    }

    val altinnSRRService = AltinnSRRService(environment) {
        when (environment.application.localEnv != "localWin") {
            true -> Clients.iRegisterSRRAgencyExternalBasic(environment.altinn.altinnSrrUrl).apply {
                when (environment.application.devProfile) {
                    true -> stsClient.configureFor(this, STS_SAML_POLICY_NO_TRANSPORT_BINDING)
                    false -> stsClient.configureFor(this)
                }
            }
            false -> Clients.iRegisterSRRAgencyExternalBasic(environment.altinn.altinnSrrUrl)
        }
    }
    val altinnDqService = AltinnDQService(environment) {
        when (environment.application.localEnv != "localWin") {
            true -> Clients.iDownloadQueueExternalBasic(environment.altinn.altinnDqUrl).apply {
                when (environment.application.devProfile) {
                    true -> stsClient.configureFor(this, STS_SAML_POLICY_NO_TRANSPORT_BINDING)
                    false -> stsClient.configureFor(this)
                }
            }
            false -> Clients.iDownloadQueueExternalBasic(environment.altinn.altinnDqUrl)
        }
    }
    val altinnCorrespondenceService = AltinnCorrespondenceService(environment) {
        when (environment.application.localEnv != "localWin") {
            true -> Clients.iCorrespondenceExternalBasic(environment.altinn.altinnCorrespondenceUrl).apply {
                when (environment.application.devProfile) {
                    true -> stsClient.configureFor(this, STS_SAML_POLICY_NO_TRANSPORT_BINDING)
                    false -> stsClient.configureFor(this)
                }
            }
            false -> Clients.iCorrespondenceExternalBasic(environment.altinn.altinnCorrespondenceUrl)
        }
    }
    val altinnPrefillService = AltinnPrefillService(environment) {
        when (environment.application.localEnv != "localWin") {
            true -> Clients.iPrefillExternalBasic(environment.altinn.altinnPrefillUrl).apply {
                when (environment.application.devProfile) {
                    true -> stsClient.configureFor(this, STS_SAML_POLICY_NO_TRANSPORT_BINDING)
                    false -> stsClient.configureFor(this)
                }
            }
            false -> Clients.iPrefillExternalBasic(environment.altinn.altinnPrefillUrl)
        }
    }
    val altinnReceiptService = AltinnReceiptService(environment) {
        when (environment.application.localEnv != "localWin") {
            true -> Clients.iReceiptAgencyExternalBasic(environment.altinn.altinnReceiptUrl).apply {
                when (environment.application.devProfile) {
                    true -> stsClient.configureFor(this, STS_SAML_POLICY_NO_TRANSPORT_BINDING)
                    false -> stsClient.configureFor(this)
                }
            }
            false -> Clients.iReceiptAgencyExternalBasic(environment.altinn.altinnReceiptUrl)
        }
    }

    val expireAlerts = ExpireAlerts(environment, applicationState, altinnSRRService)
    if (!environment.application.devProfile) {
        launch(backgroundTasksContext) {
            expireAlerts.checkDates()
        }
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
        ssrAPI(altinnSrrService = altinnSRRService, environment = environment)
        logger.info { "Installing altinn dq api" }
        dqAPI(altinnDqService = altinnDqService, environment = environment)
        logger.info { "Installing altinn correspondence api" }
        correspondenceAPI(altinnCorrespondenceService = altinnCorrespondenceService, environment = environment)
        logger.info { "Installing altinn receipts api" }
        prefillAPI(altinnPrefillService = altinnPrefillService, environment = environment)
        receiptsAPI(altinnReceiptService = altinnReceiptService, environment = environment)
        nais(readinessCheck = { applicationState.initialized }, livenessCheck = { applicationState.running })
    }
}
