package no.nav.altinn.admin.service.quick.payout

import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.coroutines.delay
import mu.KotlinLogging
import no.nav.altinn.admin.Environment
import no.nav.altinn.admin.common.ApplicationState
import no.nav.altinn.admin.common.objectMapper
import no.nav.altinn.admin.metrics.Metrics
import no.nav.altinn.admin.service.dq.AltinnDQService
import java.io.StringReader
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
    suspend fun fetchAndCreateList() {
        while (applicationState.running) {
            val today = Calendar.getInstance().time
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
            val result = QuickList(quickList.size, quickList)
            Metrics.quickPayoutSuccess.labels("5546").inc()
            logger.info { "Result for output from DQ : " + objectMapper.writeValueAsString(result) }
            delay(1000 * 60 * 1) // wait a minute
//            response.items.forEach { ar ->
//                if (failedAR.contains(ar.archiveReference)) {
//                    return@forEach // skip failed message download
//                }
//                val purgeResponse = dqService.purgeItem(ar.archiveReference)
//                if (purgeResponse.status == "Ok") {
//                    logger.debug { "Message ${ar.archiveReference} deleted from DQ." }
//                } else {
//                    logger.error { "Failed to delete ${ar.archiveReference}, will come again next day." }
//                }
//            }
            delay(1000 * 60 * 60 * 24)
        }
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
    @JsonProperty("hyppigUtbetalig")
    val hyppigUtbetalig: Boolean
)