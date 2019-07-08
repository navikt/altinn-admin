package no.nav.altinn.admin.service.alerts

import kotlinx.coroutines.delay
import mu.KotlinLogging
import no.nav.altinn.admin.Environment
import no.nav.altinn.admin.common.ApplicationState
import no.nav.altinn.admin.metrics.Metrics
import no.nav.altinn.admin.service.srr.AltinnSRRService
import org.joda.time.DateTime
import java.util.*

private val logger = KotlinLogging.logger { }
private val regexExpireDate = """\d+[yY]\d+[mM]\d+[dD]""".toRegex()

class ExpireAlerts(
    private val env: Environment,
    private val applicationState: ApplicationState,
    private val altinnSRRService: AltinnSRRService
) {
    suspend fun checkDates() {
        while (applicationState.running) {
            val today = Calendar.getInstance().time
            val expires = getRelativeExpireDate(env.srrExpireDate)
            logger.debug { "Running thread check dates...$today" }

            val serviceCodes = env.application.serviceCodes.split(",")
            serviceCodes.forEach { sc ->
                logger.debug { "Fetching rules for serviceCode $sc" }
                val responseList = altinnSRRService.getRightsForAllBusinesses(sc)
                val currentExpired = Metrics.srrExipingRightsRules.labels(sc).get()
                var numberOfExpiredRules = 0
                responseList.register.register.forEach {
                    val dd = DateTime.parse(it.tilDato).toCalendar(Locale.getDefault())
                    if (expires > dd) {
                        numberOfExpiredRules++
                        logger.warn { "Rule is about to expire or expired already : ${it.organisasjonsnummer} - with domene ${it.domene} - has date ${it.tilDato} !" }
                    }
                    logger.debug { "${it.organisasjonsnummer} - with domene ${it.domene} - has date ${it.tilDato}" }
                }
                if (numberOfExpiredRules > 0 && currentExpired == 0.0) {
                    logger.debug { "$sc ADD expiring: $numberOfExpiredRules" }
                    Metrics.srrExipingRightsRules.labels(sc).inc(numberOfExpiredRules.toDouble())
                }
                if (numberOfExpiredRules == 0 && currentExpired > 0.0) {
                    logger.debug { "$sc REMOVE expiring: $currentExpired" }
                    Metrics.srrExipingRightsRules.labels(sc).dec(currentExpired)
                }
                if (numberOfExpiredRules > 0 && currentExpired > 0) {
                    val diff = numberOfExpiredRules - currentExpired
                    logger.debug { "$sc UPDATE expiring: $diff" }
                    Metrics.srrExipingRightsRules.labels(sc).inc(diff)
                }
                logger.debug { "Done fetching rules for serviceCode $sc" }
            }
            delay(1000*60*60*24)
        }
    }
}

fun getRelativeExpireDate(srrExpire: String): Calendar {
    val expires = Calendar.getInstance()
    if (regexExpireDate.matches(srrExpire)) {
        val lsrr = srrExpire.toLowerCase()
        val values = lsrr.split("y", "m", "d")
        expires.add(Calendar.YEAR, values[0].toInt())
        expires.add(Calendar.MONTH, values[1].toInt())
        expires.add(Calendar.DATE, values[2].toInt())
    } else {
        logger.warn { "No expired date configured or given in wrong format, defaulting to 2 months expiring date warning." }
        expires.add(Calendar.YEAR, 0)
        expires.add(Calendar.MONTH, 2)
        expires.add(Calendar.DATE, 0)
    }

    return expires
}
