package no.nav.altinn.admin.service.dq

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.logging.DEFAULT
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logger
import io.ktor.client.features.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.createTestEnvironment
import io.ktor.server.testing.handleRequest
import java.nio.file.Paths
import kotlin.test.BeforeTest
import kotlin.test.Test
import no.nav.altinn.admin.Environment
import no.nav.altinn.admin.common.ApplicationState
import no.nav.altinn.admin.common.objectMapper
import no.nav.altinn.admin.installAuthentication
import no.nav.altinn.admin.installCommon
import org.amshove.kluent.shouldBeEqualTo

@KtorExperimentalLocationsAPI
class AltinnDQServiceTest {
    val applicationState = ApplicationState(running = true, initialized = true)
    val engine = TestApplicationEngine(createTestEnvironment())
    val testEnvironment = Environment()

    @BeforeTest
    fun beforeTest() {
        engine.start(wait = false)
        val httpClient = HttpClient(CIO) {
            install(JsonFeature) {
                serializer = JacksonSerializer { objectMapper }
            }
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.NONE
            }
        }
        val issuerUrl = "https://sts.issuer.net/myid"

        val path = "src/test/resources/jwkset.json"
        val uri = Paths.get(path).toUri().toURL()
        val jwkProvider = JwkProviderBuilder(uri).build()
        val consumerClientId = "1"
        val acceptedClientId = "2"
        val notAcceptedClientId = "4"
        engine.application.installAuthentication(testEnvironment, httpClient, jwkProvider, issuerUrl, acceptedClientId)
        engine.application.installCommon(testEnvironment, applicationState, httpClient)
    }

    @Test
    fun `Hent elementer i DownloadQueue med ugyldig tjeneste`() {
        with(engine) {
            val params = "Samtykke_UFOR"
            val req = handleRequest(HttpMethod.Get, "/api/v1/altinn/dq/elementer/tjeneste/$params") {
                addHeader(HttpHeaders.Accept, "application/json")
                addHeader("Content-Type", "application/json")
                addHeader(HttpHeaders.Authorization, "Bearer 1234567890")
            }
            req.response.status() shouldBeEqualTo HttpStatusCode.InternalServerError
        }
    }

    @Test
    fun `Hent elementer fra DownloadQueue med ugyldig utgave kode`() {
        with(engine) {
            val params = "Peek_Mentor"
            val req = handleRequest(HttpMethod.Get, "/api/v1/altinn/dq/elementer/tjeneste/$params") {
                addHeader(HttpHeaders.Accept, "application/json")
                addHeader("Content-Type", "application/json")
                addHeader(HttpHeaders.Authorization, "Bearer 1234567890")
            }

            req.response.status() shouldBeEqualTo HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `Hent melding fra DownloadQueue med tomt AR nummer`() {
        with(engine) {
            val arNummer = " "
            val req = handleRequest(HttpMethod.Get, "/api/v1/altinn/dq/hent/$arNummer") {
                addHeader(HttpHeaders.Accept, "application/json")
                addHeader("Content-Type", "application/json")
                addHeader(HttpHeaders.Authorization, "Bearer 1234567890")
            }

            req.response.status() shouldBeEqualTo HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `Slett melding fra DownloadQueue med tomt AR nummer`() {
        with(engine) {
            val arNummer = " "
            val req = handleRequest(HttpMethod.Delete, "/api/v1/altinn/dq/slett/$arNummer") {
                addHeader("Content-Type", "application/json")
                addHeader(HttpHeaders.Authorization, "Bearer 1234567890")
            }

            req.response.status() shouldBeEqualTo HttpStatusCode.BadRequest
        }
    }
}
