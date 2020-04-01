package no.nav.altinn.admin.service.receipt

import mu.KotlinLogging
import no.altinn.schemas.services.intermediary.receipt._2009._10.ReceiptStatusEnum
import no.altinn.schemas.services.intermediary.receipt._2015._06.ReceiptType
import no.altinn.schemas.services.intermediary.shipment._2009._10.ReferenceType
import no.altinn.services.intermediary.receipt._2009._10.IReceiptAgencyExternalBasic
import no.altinn.services.intermediary.receipt._2009._10.IReceiptAgencyExternalBasicGetReceiptListBasicV2AltinnFaultFaultFaultMessage
import no.nav.altinn.admin.Environment
import no.nav.altinn.admin.common.isDate
import no.nav.altinn.admin.common.toXmlGregorianCalendar

private val logger = KotlinLogging.logger { }
class AltinnReceiptService(env: Environment, iReceiptAgencyExternalBasicFactory: () -> IReceiptAgencyExternalBasic) {

    private val altinnUsername = env.altinn.username
    private val altinnUserPassword = env.altinn.password
    private val iReceiptAgencyExternalBasic by lazy(iReceiptAgencyExternalBasicFactory)

    fun getInboundReceipts(dateFrom: String, dateTo: String): ReceiptItems {
        try {
            if (!isDate(dateFrom) || !isDate(dateTo))
                throw IllegalArgumentException("Wrong date format")

            val d1 = toXmlGregorianCalendar(dateFrom)
            val d2 = toXmlGregorianCalendar(dateTo)
            val receiptItems = iReceiptAgencyExternalBasic.getReceiptListBasicV2(altinnUsername, altinnUserPassword,
                ReceiptType.FORM_TASK, d1, d2)

            var arList = mutableListOf<String>()
            for (receipt in receiptItems.receipt) {
                if (receipt.receiptStatus != ReceiptStatusEnum.OK)
                    continue

                val ar = receipt.references.reference.find { it.referenceType == ReferenceType.ARCHIVE_REFERENCE }?.referenceValue
                if (ar != null) {
                    arList.add(ar)
                }
            }
            return ReceiptItems("Ok", arList.size, arList)
        } catch (e: IReceiptAgencyExternalBasicGetReceiptListBasicV2AltinnFaultFaultFaultMessage) {
            no.nav.altinn.admin.service.receipt.logger.error { "Exception IReceiptAgencyExternalBasic.getReceiptListBasicV2\n" +
                "\n ErrorMessage  ${e.faultInfo.altinnErrorMessage}" +
                "\n ExtendedErrorMessage  ${e.faultInfo.altinnExtendedErrorMessage}" +
                "\n LocalizedErrorMessage  ${e.faultInfo.altinnLocalizedErrorMessage}" +
                "\n ErrorGuid  ${e.faultInfo.errorGuid}" +
                "\n UserGuid  ${e.faultInfo.userGuid}" +
                "\n UserId  ${e.faultInfo.userId}"
            }
            return ReceiptItems("Failed", 0, emptyList())
        }
    }

    fun getCorrespondenceReceipts(dateFrom: String, dateTo: String): CorrespondenceReceiptItems {
        try {
            if (!isDate(dateFrom) || !isDate(dateTo))
                throw IllegalArgumentException("Wrong date format")
            val d1 = toXmlGregorianCalendar(dateFrom)
            val d2 = toXmlGregorianCalendar(dateTo)
            val receiptItems = iReceiptAgencyExternalBasic.getReceiptListBasicV2(altinnUsername, altinnUserPassword,
                ReceiptType.CORRESPONDENCE, d1, d2).receipt

            var receiptIdList = mutableListOf<CorrespondenceReceipt>()
            for (receipt in receiptItems) {
                if (receipt.receiptStatus != ReceiptStatusEnum.OK) {
                    continue
                }

                val esr = receipt.references.reference.find { it.referenceType == ReferenceType.EXTERNAL_SHIPMENT_REFERENCE }?.referenceValue
                    ?: continue

                receiptIdList.add(CorrespondenceReceipt(receipt.receiptId.toString(), esr))
            }
            return CorrespondenceReceiptItems("Ok", receiptIdList.size, receiptIdList)
        } catch (e: IReceiptAgencyExternalBasicGetReceiptListBasicV2AltinnFaultFaultFaultMessage) {
            no.nav.altinn.admin.service.receipt.logger.error { "Exception IReceiptAgencyExternalBasic.getReceiptListBasicV2\n" +
                "\n ErrorMessage  ${e.faultInfo.altinnErrorMessage}" +
                "\n ExtendedErrorMessage  ${e.faultInfo.altinnExtendedErrorMessage}" +
                "\n LocalizedErrorMessage  ${e.faultInfo.altinnLocalizedErrorMessage}" +
                "\n ErrorGuid  ${e.faultInfo.errorGuid}" +
                "\n UserGuid  ${e.faultInfo.userGuid}" +
                "\n UserId  ${e.faultInfo.userId}"
            }
            return CorrespondenceReceiptItems("Failed", 0, emptyList())
        }
    }
}