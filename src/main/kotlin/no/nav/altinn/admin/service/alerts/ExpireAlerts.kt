package no.nav.altinn.admin.service.alerts

import kotlinx.coroutines.delay
import mu.KotlinLogging
import no.nav.altinn.admin.Environment
import no.nav.altinn.admin.common.ApplicationState
import no.nav.altinn.admin.service.srr.AltinnSRRService
import org.joda.time.DateTime

private val logger = KotlinLogging.logger { }

class ExpireAlerts(
    private val env: Environment,
    private val applicationState: ApplicationState,
    private val altinnSRRService: AltinnSRRService
) {
    suspend fun checkDates() {
        while (applicationState.running) {
            val dateTime = DateTime.now()
            logger.info { "Running thread check dates...$dateTime" }
            val serviceCodes = env.application.serviceCodes.split(",")
            logger.info { "...fetching " }
            serviceCodes.forEach {
                logger.info { "...fetching rules for serviceCode $it" }
                val responseList = altinnSRRService.getRightsForAllBusinesses(it)
                responseList.register.register.forEach {
                    logger.info { "${it.organisasjonsnummer} - with domene ${it.domene} - has date ${it.tilDato} !" }
                }
                logger.info { "done fetching rules for serviceCode $it" }
            }

            delay(60_000L)
        }
    }
}
