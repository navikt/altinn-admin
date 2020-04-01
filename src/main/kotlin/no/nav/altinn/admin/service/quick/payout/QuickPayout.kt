package no.nav.altinn.admin.service.quick.payout

import com.fasterxml.jackson.annotation.JsonProperty
import mu.KotlinLogging
import no.nav.altinn.admin.Environment
import no.nav.altinn.admin.common.ApplicationState
import no.nav.altinn.admin.common.objectMapper
import no.nav.altinn.admin.metrics.Metrics
import no.nav.altinn.admin.service.dq.AltinnDQService
import java.io.StringReader
import java.lang.Thread.sleep
import java.util.*
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.events.XMLEvent

private val logger = KotlinLogging.logger { }
private val xmlInputFactory = XMLInputFactory.newInstance()

class QuickPayout(
    private val env: Environment,
    private val applicationState: ApplicationState,
    private val dqService: AltinnDQService
) {
    fun fetchAndCreateList() {
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 22)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                logger.debug { "Running thread fetch quick payout records and create list...$today" }
                val response = dqService.getDownloadQueueItems("5546", "1")

                val quickList = mutableListOf<Quick>()
                val failedAR = mutableListOf<String>()
                response.items.forEach item@{ ar ->
                    logger.debug { "Fetching rules for serviceCode $ar" }
                    val melding = dqService.getFormData(ar.archiveReference)
                    if (melding.status == "Ok") {
                        logger.debug { "Message received from DQ ${ar.archiveReference}." }
                    } else {
                        logger.error { "Failed to get message ${ar.archiveReference}, from DQ." }
                        failedAR.add(ar.archiveReference)
                        return@item
                    }

                    val xml = melding.arData.formData
                    val xmlReader = xmlInputFactory.createXMLStreamReader(StringReader(xml))
                    var organisasjonsnummer = ""
                    var hyppig = false
                    while (xmlReader.hasNext()) {
                        val eventType = xmlReader.next()
                        if (eventType == XMLEvent.START_ELEMENT) {
                            when (xmlReader.localName) {
                                "organisasjonsnummer" -> organisasjonsnummer = xmlReader.elementText
                                "onskerHyppigereRefusjon" -> hyppig = xmlReader.elementText == "true"
                            }
                        }
                    }
                    quickList.add(Quick(organisasjonsnummer, hyppig))
                    logger.debug { "Got xml data $xml" }
                }
                if (quickList.size > 0) {
                    val result = QuickList(quickList.size, quickList)
                    val po = objectMapper.writeValueAsString(result)
                    Metrics.quickPayoutSuccess.labels("5546", po).inc()
                    logger.info { "Resultat hyppig utbetaling liste: $po" }
                } else {
                    logger.info { "Ingen nye meldinger funnet, bedre lykke neste gang." }
                }
                sleep(1000 * 60) // wait a minute
                response.items.forEach { ar ->
                    if (failedAR.contains(ar.archiveReference)) {
                        return@forEach // skip failed message download
                    }
                    val purgeResponse = dqService.purgeItem(ar.archiveReference)
                    if (purgeResponse.status == "Ok") {
                        logger.debug { "Message ${ar.archiveReference} deleted from DQ." }
                    } else {
                        logger.error { "Failed to delete ${ar.archiveReference}, will come again next day." }
                    }
                }
            }
        }, today.time, 1000 * 60 * 60 * 24)
    }
}

data class QuickList(
    @JsonProperty("antall")
    val antall: Int,
    @JsonProperty("liste")
    val liste: List<Quick>
)

data class Quick(
    @JsonProperty("organisasjonsnummer")
    val organisasjonsnummer: String,
    @JsonProperty("hyppigUtbetaling")
    val hyppigUtbetaling: Boolean
)