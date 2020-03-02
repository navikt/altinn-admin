package no.nav.altinn.admin.service.correspondence

import mu.KotlinLogging
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.InsertCorrespondenceV2
import no.altinn.schemas.services.serviceengine.correspondence._2016._02.CorrespondenceStatusFilterV3
import no.altinn.services.serviceengine.correspondence._2009._10.ICorrespondenceAgencyExternalBasic
import no.altinn.services.serviceengine.correspondence._2009._10.ICorrespondenceAgencyExternalBasicGetCorrespondenceStatusDetailsBasicV3AltinnFaultFaultFaultMessage
import no.nav.altinn.admin.Environment
import no.nav.altinn.admin.common.randomUuid
import javax.xml.datatype.XMLGregorianCalendar

private val logger = KotlinLogging.logger { }

class AltinnCorrespondenceService(private val env: Environment, iCorrepsondenceExternalBasicFactory: () -> ICorrespondenceAgencyExternalBasic) {
    private val altinnUsername = env.altinn.username
    private val altinnUserPassword = env.altinn.password
    private val SYSTEM_CODE = "NAV_ALF"
    private val iCorrespondenceExternalBasic by lazy(iCorrepsondenceExternalBasicFactory)

    fun getCorrespondenceDetails(serviceCode: String, fromDate: XMLGregorianCalendar? = null, toDate: XMLGregorianCalendar? = null, reportee: String? = ""): CorrespondenceResponse {
        val corrFilter = CorrespondenceStatusFilterV3()
        corrFilter.serviceCode = serviceCode
        corrFilter.serviceEditionCode = 1
        if (fromDate != null)
            corrFilter.createdAfterDate = fromDate
        if (toDate != null)
            corrFilter.createdBeforeDate = toDate
        if (!reportee.isNullOrEmpty())
            corrFilter.reportee = reportee

        try {
            val results = iCorrespondenceExternalBasic.getCorrespondenceStatusDetailsBasicV3(altinnUsername, altinnUserPassword,
                corrFilter).correspondenceStatusInformation.correspondenceStatusDetailsList.statusV2
            var correspondenceDetails = mutableListOf<CorrespondenceDetails>()
            for (detail in results) {
                correspondenceDetails.add(CorrespondenceDetails(
                    detail.correspondenceID,
                    detail.createdDate,
                    detail.reportee,
                    detail.statusChanges.statusChangeV2.last().statusDate,
                    detail.statusChanges.statusChangeV2.last().statusType.toString()
                ))
            }
            return CorrespondenceResponse("Ok", "Found ${results.size} correspondences", correspondenceDetails)
        } catch (e: ICorrespondenceAgencyExternalBasicGetCorrespondenceStatusDetailsBasicV3AltinnFaultFaultFaultMessage) {
            logger.error { "iCorrespondenceExternalBasic.getCorrespondenceDetails feilet \n" +
                "\n ErrorMessage  ${e.faultInfo.altinnErrorMessage}" +
                "\n ExtendedErrorMessage  ${e.faultInfo.altinnExtendedErrorMessage}" +
                "\n LocalizedErrorMessage  ${e.faultInfo.altinnLocalizedErrorMessage}" +
                "\n ErrorGuid  ${e.faultInfo.errorGuid}" +
                "\n UserGuid  ${e.faultInfo.userGuid}" +
                "\n UserId  ${e.faultInfo.userId}"
            }
        }
        return CorrespondenceResponse("Failed", "Could not get any correspondence, check log.", emptyList())
    }

    fun insertCorrespondence() {
        iCorrespondenceExternalBasic.insertCorrespondenceBasicV2(altinnUsername, altinnUserPassword, SYSTEM_CODE, randomUuid(), InsertCorrespondenceV2())
    }
}
