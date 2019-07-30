package no.nav.altinn.admin.service.dq

import mu.KotlinLogging
import no.altinn.services.archive.downloadqueue._2012._08.IDownloadQueueExternalBasic
import no.altinn.services.archive.downloadqueue._2012._08.IDownloadQueueExternalBasicGetArchivedFormTaskBasicDQAltinnFaultFaultFaultMessage
import no.nav.altinn.admin.Environment
import org.w3c.dom.Document
import org.xml.sax.InputSource
import java.io.File
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

private val logger = KotlinLogging.logger { }

class AltinnDQService(private val env: Environment, iDownloadQueueExternalBasicFactory: () -> IDownloadQueueExternalBasic) {

    private val altinnUsername = env.altinn.username
    private val altinnUserPassword = env.altinn.password
    private val iDownloadQueueExternalBasic by lazy(iDownloadQueueExternalBasicFactory)
    private val documentBuilder: DocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()

    fun getMessageFromDq(arNummer: String): DqResponse {
        try {
//            Metrics.getRightsRequest.labels(arNummer).inc()
            logger.debug { "Tries to get ar message." }
            val archivedFormTaskBasicDQ = iDownloadQueueExternalBasic.getArchivedFormTaskBasicDQ(altinnUsername, altinnUserPassword,
                arNummer, null, false)
            val dqList = env.application.dq.serviceCodes.split(",")
            if (!dqList.contains(archivedFormTaskBasicDQ.serviceCode)) {
                return DqResponse("Ok", "ServiceCode is not whitelisted: ${archivedFormTaskBasicDQ.serviceCode}")
            }
            logger.debug { "Message from DQ: $archivedFormTaskBasicDQ" }
            val doc: Document = documentBuilder.parse(
                InputSource(
                    archivedFormTaskBasicDQ.forms.archivedFormDQBE[0].formData
                        .removePrefix("<![CDATA[")
                        .removeSuffix("]]>")
                        .reader()
                )
            )

            File("$arNummer.xml").writeText(doc.textContent)

//            Metrics.getRightsResponse.labels(arNummer).inc()
            return DqResponse("Ok", "Ok")
        } catch (e: IDownloadQueueExternalBasicGetArchivedFormTaskBasicDQAltinnFaultFaultFaultMessage) {
            logger.error { "iDownloadQueueExternalBasic.getArchivedFormTaskBasicDQ feilet \n" +
                        "\n ErrorMessage  ${e.faultInfo.altinnErrorMessage}" +
                        "\n ExtendedErrorMessage  ${e.faultInfo.altinnExtendedErrorMessage}" +
                        "\n LocalizedErrorMessage  ${e.faultInfo.altinnLocalizedErrorMessage}" +
                        "\n ErrorGuid  ${e.faultInfo.errorGuid}" +
                        "\n UserGuid  ${e.faultInfo.userGuid}" +
                        "\n UserId  ${e.faultInfo.userId}"
            }
        }
//        Metrics.getRightsFailed.labels(arNummer).inc()
        return DqResponse("Failed", "Unknown error occurred when getting rights registry, check logger")
    }
}
