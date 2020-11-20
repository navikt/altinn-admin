package no.nav.altinn.admin.service.dq

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.createTestEnvironment
import io.ktor.server.testing.handleRequest
import no.nav.altinn.admin.Environment
import no.nav.altinn.admin.common.ApplicationState
import no.nav.altinn.admin.common.InMemoryLDAPServer
import no.nav.altinn.admin.common.encodeBase64
import no.nav.altinn.admin.mainModule
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object AltinnDQServiceSpek : Spek({
    val applicationState = ApplicationState(running = true, initialized = true)

    describe("Test all authorized calls") {
        context("Get Route /api/v1/altinn/dq/elementer/{tjenesteKode}") {
            val engine = TestApplicationEngine(createTestEnvironment())
            val testEnvironment = Environment()

            beforeGroup {
                InMemoryLDAPServer.start()
                engine.start(wait = false)
                engine.application.mainModule(testEnvironment, applicationState = applicationState)
            }
            with(engine) {
                context("Route /api/v1/altinn/dq/elementer/tjeneste/{tjeneste}") {
                    it("Hent elementer i DownloadQueue med ugyldig tjeneste") {
                        val params = "Samtykke_UFOR"
                        val req = handleRequest(HttpMethod.Get, "/api/v1/altinn/dq/elementer/tjeneste/$params") {
                            addHeader(HttpHeaders.Accept, "application/json")
                            addHeader("Content-Type", "application/json")
                            addHeader(HttpHeaders.Authorization, "Basic ${encodeBase64("n000001:itest1".toByteArray())}")
                        }

                        req.requestHandled shouldEqual true
                        req.response.status() shouldEqual HttpStatusCode.InternalServerError
                    }
                    it("Hent elementer fra DownloadQueue med ugyldig utgave kode") {
                        val params = "Peek_Mentor"
                        val req = handleRequest(HttpMethod.Get, "/api/v1/altinn/dq/elementer/tjeneste/$params") {
                            addHeader(HttpHeaders.Accept, "application/json")
                            addHeader("Content-Type", "application/json")
                            addHeader(HttpHeaders.Authorization, "Basic ${encodeBase64("n000001:itest1".toByteArray())}")
                        }

                        req.requestHandled shouldEqual true
                        req.response.status() shouldEqual HttpStatusCode.BadRequest
                    }
                    it("Hent melding fra DownloadQueue med tomt AR nummer") {
                        val arNummer = " "
                        val req = handleRequest(HttpMethod.Get, "/api/v1/altinn/dq/hent/$arNummer") {
                            addHeader(HttpHeaders.Accept, "application/json")
                            addHeader("Content-Type", "application/json")
                            addHeader(HttpHeaders.Authorization, "Basic ${encodeBase64("n000001:itest1".toByteArray())}")
                        }

                        req.requestHandled shouldEqual true
                        req.response.status() shouldEqual HttpStatusCode.BadRequest
                    }
                }
            }
            afterGroup {
                InMemoryLDAPServer.stop()
            }
        }
        context("Delete Route /api/v1/altinn/dq/slett/{arNummer}") {
            val engine = TestApplicationEngine(createTestEnvironment())
            val testEnvironment = Environment()

            beforeGroup {
                InMemoryLDAPServer.start()
                engine.start(wait = false)
                engine.application.mainModule(testEnvironment, applicationState = applicationState)
            }
            with(engine) {
                context("Route /api/v1/altinn/dq/slett/{arNummer}") {
                    it("Slett melding fra DownloadQueue med tomt AR nummer") {
                        val arNummer = " "
                        val req = handleRequest(HttpMethod.Delete, "/api/v1/altinn/dq/slett/$arNummer") {
                            addHeader("Content-Type", "application/json")
                            addHeader(HttpHeaders.Authorization, "Basic ${encodeBase64("n000002:itest2".toByteArray())}")
                        }

                        req.requestHandled shouldEqual true
                        req.response.status() shouldEqual HttpStatusCode.BadRequest
                    }
                }
            }
            afterGroup {
                InMemoryLDAPServer.stop()
            }
        }
    }
})
