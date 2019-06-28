package no.nav.altinn.admin.service.alerts

import kotlinx.coroutines.delay
import mu.KotlinLogging
import no.nav.altinn.admin.Environment
import no.nav.altinn.admin.common.ApplicationState
import org.joda.time.DateTime

private val logger = KotlinLogging.logger { }

class ExpireAlerts(
    private val env: Environment,
    private val applicationState: ApplicationState
) {
    suspend fun checkDates() {
        while (applicationState.running) {
            val dateTime = DateTime.now()
            logger.info { "Running thread check dates...$dateTime" }
            delay(10_000L)
        }
    }
}
