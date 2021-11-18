package no.nav.altinn.admin.api

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.routing.routing
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.it
import io.ktor.server.testing.on
import kotlin.test.Test
import org.amshove.kluent.shouldBeEqualTo

class SelfTest {

    private val selfTests = listOf(
        SelfTests.LIVENESS to "/internal/is_alive",
        SelfTests.READINESS to "/internal/is_ready"
    )

    private val testCases = listOf(
        TestCase(description = "both selftests OK", readiness = true, liveness = true),
        TestCase(description = "both selftests ERROR", readiness = false, liveness = false),
        TestCase(description = "only readiness OK", readiness = true, liveness = false),
        TestCase(description = "only liveness OK", readiness = false, liveness = true)
    )

    @Test
    fun testCases() {
        testCases.forEach { (description, readiness, liveness) ->
            with(TestApplicationEngine()) {
                start()
                application.routing {
                    nais(readinessCheck = { readiness }, livenessCheck = { liveness })
                }
                selfTests.forEach { (selfTest, url) ->
                    on("$selfTest test") {
                        with(handleRequest(HttpMethod.Get, url)) {
                            when (
                                when (selfTest) {
                                    SelfTests.LIVENESS -> liveness
                                    SelfTests.READINESS -> readiness
                                }
                            ) {
                                true -> it("should return ${HttpStatusCode.OK}") {
                                    response.status() shouldBeEqualTo HttpStatusCode.OK
                                }

                                false -> it("should return ${HttpStatusCode.InternalServerError}") {
                                    response.status() shouldBeEqualTo HttpStatusCode.InternalServerError
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private data class TestCase(val description: String, val readiness: Boolean, val liveness: Boolean)
    private enum class SelfTests { LIVENESS, READINESS }
}
