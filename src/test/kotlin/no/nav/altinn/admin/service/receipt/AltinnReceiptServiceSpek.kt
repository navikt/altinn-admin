package no.nav.altinn.admin.service.receipt

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import no.nav.altinn.admin.Environment
import no.nav.altinn.admin.common.ApplicationState
import no.nav.altinn.admin.mainModule
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object AltinnReceiptServiceSpek : Spek({
    val applicationState = ApplicationState(running = true, initialized = true)

    withTestApplication(moduleFunction = { mainModule(Environment(), applicationState) }) {
        describe("GET Hent AR kvitteringer for periode.") {
            with(handleRequest(HttpMethod.Get, "/api/v1/altinn/arkvitteringer/hent/2020-02-1f/2020-02-18")) {
                it("Hent AR kvitteringer for periode, skal feile med 'bad request'") {
                    response.status() shouldEqual HttpStatusCode.BadRequest
                }
            }
        }
    }
    withTestApplication(moduleFunction = { mainModule(Environment(), applicationState) }) {
        describe("GET Hent AR kvitteringer for periode.") {
            with(handleRequest(HttpMethod.Get, "/api/v1/altinn/arkvitteringer/hent/ /2020-02-18")) {
                it("Hent AR kvitteringer for periode, skal feile med 'bad request'") {
                    response.status() shouldEqual HttpStatusCode.BadRequest
                }
            }
        }
    }
    withTestApplication(moduleFunction = { mainModule(Environment(), applicationState) }) {
        describe("GET Hent AR kvitteringer for periode.") {
            with(handleRequest(HttpMethod.Get, "/api/v1/altinn/arkvitteringer/hent/2020-02-18/2020-02-10")) {
                it("Hent AR kvitteringer for periode, skal feile med 'bad request'") {
                    response.status() shouldEqual HttpStatusCode.BadRequest
                }
            }
        }
    }
    withTestApplication(moduleFunction = { mainModule(Environment(), applicationState) }) {
        describe("GET Hent AR kvitteringer for periode.") {
            with(handleRequest(HttpMethod.Get, "/api/v1/altinn/arkvitteringer/hent/2020-02-10/2020-02-11")) {
                it("Hent AR kvitteringer for periode, skal retunere status ok") {
                    response.status() shouldEqual HttpStatusCode.ServiceUnavailable
                }
            }
        }
    }
    withTestApplication(moduleFunction = { mainModule(Environment(), applicationState) }) {
        describe("GET Hent AR kvitteringer for periode.") {
            with(handleRequest(HttpMethod.Get, "/api/v1/altinn/meldingskvitteringer/hent/2020-02-10/2020-02-11")) {
                it("Hent meldingskvitteringer for periode, skal retunere status ok") {
                    response.status() shouldEqual HttpStatusCode.ServiceUnavailable
                }
            }
        }
    }
})
