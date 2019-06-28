package no.nav.altinn.admin.service.alerts

import kotlinx.coroutines.delay
import mu.KotlinLogging
import no.nav.altinn.admin.Environment
import no.nav.altinn.admin.common.ApplicationState
import no.nav.altinn.admin.service.srr.AltinnSRRService
import org.joda.time.DateTime
import java.util.*

private val logger = KotlinLogging.logger { }

class ExpireAlerts(
    private val env: Environment,
    private val applicationState: ApplicationState,
    private val altinnSRRService: AltinnSRRService
) {
    suspend fun checkDates() {
        while (applicationState.running) {
            val today = Calendar.getInstance().time
            val expires = Calendar.getInstance()
            expires.add(Calendar.YEAR, 0)
            expires.add(Calendar.MONTH, 9)
            expires.add(Calendar.DATE, 1)
            logger.debug { "Expires date add 9m and 1d : ${expires.time}" }
            logger.debug { "Running thread check dates...$today" }

            val serviceCodes = env.application.serviceCodes.split(",")
            logger.info { "...fetching " }
            serviceCodes.forEach { sc ->
                logger.info { "...fetching rules for serviceCode $sc" }
                val responseList = altinnSRRService.getRightsForAllBusinesses(sc)
                responseList.register.register.forEach {
                    val dd = DateTime.parse(it.tilDato).toCalendar(Locale.getDefault())
                    if (expires > dd) {
                        logger.warn { "Rule is about to expire or expired already : ${it.organisasjonsnummer} - with domene ${it.domene} - has date ${it.tilDato} !" }
                    }
                    logger.debug { "${it.organisasjonsnummer} - with domene ${it.domene} - has date ${it.tilDato}" }
                }
                logger.info { "done fetching rules for serviceCode $sc" }
            }

            delay(60_000L)
        }
    }
}
