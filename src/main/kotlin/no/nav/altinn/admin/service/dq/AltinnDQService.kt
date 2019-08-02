package no.nav.altinn.admin.service.dq

import mu.KotlinLogging
import no.altinn.services.archive.downloadqueue._2012._08.IDownloadQueueExternalBasic
import no.altinn.services.archive.downloadqueue._2012._08.IDownloadQueueExternalBasicGetArchivedFormTaskBasicDQAltinnFaultFaultFaultMessage
import no.altinn.services.archive.downloadqueue._2012._08.IDownloadQueueExternalBasicGetDownloadQueueItemsAltinnFaultFaultFaultMessage
import no.altinn.services.archive.downloadqueue._2012._08.IDownloadQueueExternalBasicPurgeItemAltinnFaultFaultFaultMessage
import no.nav.altinn.admin.Environment

private val logger = KotlinLogging.logger { }

class AltinnDQService(private val env: Environment, iDownloadQueueExternalBasicFactory: () -> IDownloadQueueExternalBasic) {

    private val altinnUsername = env.altinn.username
    private val altinnUserPassword = env.altinn.password
    private val iDownloadQueueExternalBasic by lazy(iDownloadQueueExternalBasicFactory)

    fun getFormData(arNummer: String): DqResponseFormData {
        try {
            logger.debug { "Tries to get AR message." }
            val archivedFormTaskBasicDQ = iDownloadQueueExternalBasic.getArchivedFormTaskBasicDQ(altinnUsername, altinnUserPassword,
                arNummer, null, false)
            val dqList = env.dqService.serviceCodes.split(",")
            if (!dqList.contains(archivedFormTaskBasicDQ.serviceCode)) {
                return DqResponseFormData("Ok", "ServiceCode is not whitelisted: ${archivedFormTaskBasicDQ.serviceCode}", FormData(""))
            }
            logger.debug { "Message from DQ: $archivedFormTaskBasicDQ" }
            val formData = archivedFormTaskBasicDQ.forms.archivedFormDQBE[0].formData
                .removePrefix("<![CDATA[")
                .removeSuffix("]]>")

            logger.debug { "FormData: $formData" }
            return DqResponseFormData("Ok", "Ok", FormData(formData))
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
        return DqResponseFormData("Failed", "Unknown error occurred when getting rights registry, check logger", FormData(""))
    }

    fun getDownloadQueueItems(serviceCode: String): DqItems {
        try {
            val dqItems = iDownloadQueueExternalBasic.getDownloadQueueItems(altinnUsername, altinnUserPassword, serviceCode).downloadQueueItemBE
            var dqList = mutableListOf<DqItem>()
            for (dqItem in dqItems) {
                dqList.add(DqItem(dqItem.archiveReference))
            }
            return DqItems("Ok", dqList)
        } catch (e: IDownloadQueueExternalBasicGetDownloadQueueItemsAltinnFaultFaultFaultMessage) {
            logger.error { "Exception iDownloadQueueExternalBasic.getDownloadQueueItems\n" +
                    "\n ErrorMessage  ${e.faultInfo.altinnErrorMessage}" +
                    "\n ExtendedErrorMessage  ${e.faultInfo.altinnExtendedErrorMessage}" +
                    "\n LocalizedErrorMessage  ${e.faultInfo.altinnLocalizedErrorMessage}" +
                    "\n ErrorGuid  ${e.faultInfo.errorGuid}" +
                    "\n UserGuid  ${e.faultInfo.userGuid}" +
                    "\n UserId  ${e.faultInfo.userId}"
            }
            return DqItems("Failed", emptyList())
        }
        return DqItems("Failed", emptyList())
    }

    fun purgeItem(archiveReference: String): DqPurge {
        try {
            iDownloadQueueExternalBasic.purgeItem(altinnUsername, altinnUserPassword, archiveReference)
            return DqPurge("Ok", "Deletet item $archiveReference")
        } catch (e: IDownloadQueueExternalBasicPurgeItemAltinnFaultFaultFaultMessage) {
            logger.error {
                "Exception iDownloadQueueExternalBasic.purgeItem\n" +
                    "\n ErrorMessage  ${e.faultInfo.altinnErrorMessage}" +
                    "\n ExtendedErrorMessage  ${e.faultInfo.altinnExtendedErrorMessage}" +
                    "\n LocalizedErrorMessage  ${e.faultInfo.altinnLocalizedErrorMessage}" +
                    "\n ErrorGuid  ${e.faultInfo.errorGuid}" +
                    "\n UserGuid  ${e.faultInfo.userGuid}" +
                    "\n UserId  ${e.faultInfo.userId}"
            }
            return DqPurge("Failed", "Error when purge item : ${e.message}")
        }
        return DqPurge("Failed", "Unknown error when purge item, check logs")
    }
}
