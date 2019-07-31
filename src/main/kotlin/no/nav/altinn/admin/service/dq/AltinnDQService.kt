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

    fun getMessageFromDq(arNummer: String): DqResponseFormData {
        try {
//            Metrics.getRightsRequest.labels(arNummer).inc()
            logger.info { "Tries to get AR message." }
            val archivedFormTaskBasicDQ = iDownloadQueueExternalBasic.getArchivedFormTaskBasicDQ(altinnUsername, altinnUserPassword,
                arNummer, null, false)
            val dqList = env.application.dq.serviceCodes.split(",")
            if (!dqList.contains(archivedFormTaskBasicDQ.serviceCode)) {
                return DqResponseFormData("Ok", "ServiceCode is not whitelisted: ${archivedFormTaskBasicDQ.serviceCode}", "")
            }
            logger.info { "Message from DQ: $archivedFormTaskBasicDQ" }
            val formData = archivedFormTaskBasicDQ.forms.archivedFormDQBE[0].formData
                .removePrefix("<![CDATA[")
                .removeSuffix("]]>")
            val doc: Document = documentBuilder.parse(InputSource(formData.reader()))

            val file = File.createTempFile("$arNummer", "xml")
            file.writeText(formData)
            logger.info { "File ${file.absolutePath} written, ${file.name}" }

//            Metrics.getRightsResponse.labels(arNummer).inc()
            return DqResponseFormData("Ok", "Ok", formData)
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
        return DqResponseFormData("Failed", "Unknown error occurred when getting rights registry, check logger", "")
    }
}
