package no.nav.altinn.admin.service.receipt

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.logging.DEFAULT
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logger
import io.ktor.client.features.logging.Logging
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
class AltinnReceiptServiceTest {
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
    fun `Hent AR kvitteringer for periode med feil fom dato`() {
        with(engine) {
            with(handleRequest(HttpMethod.Get, "/api/v1/altinn/arkvitteringer/hent/2020-02-1f/2020-02-18")) {
                response.status() shouldBeEqualTo HttpStatusCode.BadRequest
            }
        }
    }

    @Test
    fun `Hent AR kvitteringer for periode med tom fom dato`() {
        with(engine) {
            with(handleRequest(HttpMethod.Get, "/api/v1/altinn/arkvitteringer/hent/ /2020-02-18")) {
                response.status() shouldBeEqualTo HttpStatusCode.BadRequest
            }
        }
    }

    @Test
    fun `Hent AR kvitteringer for periode fom dato etter tom dato`() {
        with(engine) {
            with(handleRequest(HttpMethod.Get, "/api/v1/altinn/arkvitteringer/hent/2020-02-18/2020-02-10")) {
                response.status() shouldBeEqualTo HttpStatusCode.BadRequest
            }
        }
    }

    @Test
    fun `Hent AR kvitteringer for periode`() {
        with(engine) {
            with(handleRequest(HttpMethod.Get, "/api/v1/altinn/arkvitteringer/hent/2020-02-1f/2020-02-18")) {
                response.status() shouldBeEqualTo HttpStatusCode.BadRequest
            }
        }
    }

//    withTestApplication(moduleFunction = { mainModule(Environment(), applicationState) }) {
//        describe("GET Hent AR kvitteringer for periode.") {
//            with(handleRequest(HttpMethod.Get, "/api/v1/altinn/arkvitteringer/hent/2020-02-10/2020-02-11")) {
//                it("Hent AR kvitteringer for periode, skal retunere status ok") {
//                    response.status() shouldEqual HttpStatusCode.OK
//                }
//            }
//        }
//    }
//    withTestApplication(moduleFunction = { mainModule(Environment(), applicationState) }) {
//        describe("GET Hent AR kvitteringer for periode.") {
//            with(handleRequest(HttpMethod.Get, "/api/v1/altinn/meldingskvitteringer/hent/2020-02-10/2020-02-11")) {
//                it("Hent meldingskvitteringer for periode, skal retunere status ok") {
//                    response.status() shouldEqual HttpStatusCode.OK
//                }
//            }
//        }
//    }
}
