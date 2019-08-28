package no.nav.altinn.admin.service.dq

import mu.KotlinLogging
import no.altinn.services.archive.downloadqueue._2012._08.IDownloadQueueExternalBasic
import no.altinn.services.archive.downloadqueue._2012._08.IDownloadQueueExternalBasicGetArchivedFormTaskBasicDQAltinnFaultFaultFaultMessage
import no.altinn.services.archive.downloadqueue._2012._08.IDownloadQueueExternalBasicGetDownloadQueueItemsAltinnFaultFaultFaultMessage
import no.altinn.services.archive.downloadqueue._2012._08.IDownloadQueueExternalBasicPurgeItemAltinnFaultFaultFaultMessage
import no.nav.altinn.admin.Environment
import no.nav.altinn.admin.common.encodeBase64

private val logger = KotlinLogging.logger { }

class AltinnDQService(private val env: Environment, iDownloadQueueExternalBasicFactory: () -> IDownloadQueueExternalBasic) {

    private val altinnUsername = env.altinn.username
    private val altinnUserPassword = env.altinn.password
    private val iDownloadQueueExternalBasic by lazy(iDownloadQueueExternalBasicFactory)

    fun getFormData(arNummer: String): DqResponseFormTask {
        try {
            logger.debug { "Tries to get AR message." }
            val dq = iDownloadQueueExternalBasic.getArchivedFormTaskBasicDQ(altinnUsername, altinnUserPassword,
                arNummer, null, false)
            val dqList = env.dqService.serviceCodes.split(",")
            if (!dqList.contains(dq.serviceCode)) {
                return DqResponseFormTask("Failed", "ServiceCode is not whitelisted: ${dq.serviceCode}", ArData())
            }
            logger.debug { "Message from DQ: $dq" }
            val formData = dq.forms.archivedFormDQBE[0].formData
                .removePrefix("<![CDATA[")
                .removeSuffix("]]>")
            logger.debug { "FormData: $formData" }

            val attachments = dq.attachments.archivedAttachmentDQBE
            logger.info { "DownloadQueue: processing '${attachments.size}' attachments for AR: '$arNummer'" }
            val atMents = mutableListOf<Attachment>()
            attachments.forEachIndexed { index, attachment ->
                atMents.add(Attachment(
                    attachment.fileName,
                    index,
                    encodeBase64(attachment.attachmentData),
                    attachment.attachmentData.size,
                    attachment.isIsEncrypted,
                    attachment.attachmentType))
            }
            return DqResponseFormTask("Ok", "Ok",
                ArData(dq.archiveReference,
                    dq.archiveTimeStamp.toString(),
                    dq.serviceCode,
                    dq.serviceEditionCode,
                    dq.forms.archivedFormDQBE[0].dataFormatID,
                    dq.forms.archivedFormDQBE[0].dataFormatVersionID,
                    dq.formsInResponse,
                    dq.attachmentsInResponse,
                    formData,
                    Attachments(atMents)))
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
        return DqResponseFormTask("Failed", "Unknown error occurred when getting rights registry, check logger", ArData())
    }

    fun getDownloadQueueItems(serviceCode: String, serviceEdtionCode: String): DqItems {
        try {
            val dqItems = iDownloadQueueExternalBasic.getDownloadQueueItems(altinnUsername, altinnUserPassword, serviceCode).downloadQueueItemBE
            var dqList = mutableListOf<DqItem>()
            for (dqItem in dqItems) {
                if (serviceEdtionCode.isNotEmpty() && serviceEdtionCode != dqItem.serviceEditionCode.toString())
                    continue
                dqList.add(DqItem(dqItem.archiveReference,
                    dqItem.serviceCode,
                    dqItem.serviceEditionCode.toString()
                ))
            }
            return DqItems("Ok", dqList.size, dqList)
        } catch (e: IDownloadQueueExternalBasicGetDownloadQueueItemsAltinnFaultFaultFaultMessage) {
            logger.error { "Exception iDownloadQueueExternalBasic.getDownloadQueueItems\n" +
                    "\n ErrorMessage  ${e.faultInfo.altinnErrorMessage}" +
                    "\n ExtendedErrorMessage  ${e.faultInfo.altinnExtendedErrorMessage}" +
                    "\n LocalizedErrorMessage  ${e.faultInfo.altinnLocalizedErrorMessage}" +
                    "\n ErrorGuid  ${e.faultInfo.errorGuid}" +
                    "\n UserGuid  ${e.faultInfo.userGuid}" +
                    "\n UserId  ${e.faultInfo.userId}"
            }
            return DqItems("Failed", 0, emptyList())
        }
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
    }
}
