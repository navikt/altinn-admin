package no.nav.altinn.admin.service.srr

import mu.KotlinLogging
import no.altinn.schemas.services.register._2015._06.OperationResult
import no.altinn.schemas.services.register._2015._06.RegisterSRRRightsType
import no.altinn.schemas.services.register.srr._2015._06.*
import no.altinn.services.register.srr._2015._06.IRegisterSRRAgencyExternalBasic
import no.altinn.services.register.srr._2015._06.IRegisterSRRAgencyExternalBasicAddRightsBasicAltinnFaultFaultFaultMessage
import no.altinn.services.register.srr._2015._06.IRegisterSRRAgencyExternalBasicDeleteRightsBasicAltinnFaultFaultFaultMessage
import no.altinn.services.register.srr._2015._06.IRegisterSRRAgencyExternalBasicGetRightsBasicAltinnFaultFaultFaultMessage
import no.nav.altinn.admin.Environment
import javax.xml.datatype.DatatypeFactory
import java.time.ZonedDateTime
import java.util.GregorianCalendar

private val logger = KotlinLogging.logger { }

class AltinnSRRService(private val env: Environment, iRegisterSRRAgencyExternalBasicFactory: () -> IRegisterSRRAgencyExternalBasic) {

    private val altinnUsername = env.altinn.username
    private val altinnUserPassword = env.altinn.password
    private val iRegisterSRRAgencyExternalBasic: IRegisterSRRAgencyExternalBasic by lazy(iRegisterSRRAgencyExternalBasicFactory)

    fun addRights(serviceCode: String, reportee: String, redirectDomain: String, type: RegisterSRRRightsType): RightsResponse {
        logger.info { "Adding $type rights for business number $reportee with redirect url $redirectDomain." }
        try {
            val response = env.mock.srrAddResponse ?: iRegisterSRRAgencyExternalBasic.addRightsBasic(altinnUsername, altinnUserPassword, serviceCode, 1,
                    createAddRightsList(reportee, redirectDomain, type))

            return createAddResponseMessage(response, type, reportee)
        } catch (e: IRegisterSRRAgencyExternalBasicAddRightsBasicAltinnFaultFaultFaultMessage) {
            logger.error {
                "IRegisterSRRAgencyExternalBasic.addRightsBasic feilet \n" +
                        "\n ErrorMessage  ${e.faultInfo.altinnErrorMessage}" +
                        "\n ExtendedErrorMessage  ${e.faultInfo.altinnExtendedErrorMessage}" +
                        "\n LocalizedErrorMessage  ${e.faultInfo.altinnLocalizedErrorMessage}" +
                        "\n ErrorGuid  ${e.faultInfo.errorGuid}" +
                        "\n UserGuid  ${e.faultInfo.userGuid}" +
                        "\n UserId  ${e.faultInfo.userId}"
            }
        }
        return RightsResponse("Failed", "Unknown error occurred when adding $type rights, check logger")
    }

    fun deleteRights(serviceCode: String, reportee: String, redirectDomain: String, type: RegisterSRRRightsType): RightsResponse {
        logger.info { "Removing read rights for business number $reportee with redirect url $redirectDomain." }
        try {
            val response = env.mock.srrDeleteResponse ?: iRegisterSRRAgencyExternalBasic.deleteRightsBasic(altinnUsername, altinnUserPassword, serviceCode, 1,
                    createDeleteRightsList(reportee, redirectDomain, type))
            return createDeleteResponseMessage(response, type, reportee)
        } catch (e: IRegisterSRRAgencyExternalBasicDeleteRightsBasicAltinnFaultFaultFaultMessage) {
            logger.error { "IRegisterSRRAgencyExternalBasic.deleteRightsBasic feilet \n" +
                        "\n ErrorMessage  ${e.faultInfo.altinnErrorMessage}" +
                        "\n ExtendedErrorMessage  ${e.faultInfo.altinnExtendedErrorMessage}" +
                        "\n LocalizedErrorMessage  ${e.faultInfo.altinnLocalizedErrorMessage}" +
                        "\n ErrorGuid  ${e.faultInfo.errorGuid}" +
                        "\n UserGuid  ${e.faultInfo.userGuid}" +
                        "\n UserId  ${e.faultInfo.userId}"
            }
        }
        return RightsResponse("Failed", "Unknown error occurred when removing $type rights, check logger")
    }

    fun getRightsForAllBusinesses(tjenesteKode: String): RightsResponse {
        try {
            logger.info { "Tries to get all righsts..." }
            val register = env.mock.srrGetResponse ?: iRegisterSRRAgencyExternalBasic.getRightsBasic(altinnUsername, altinnUserPassword,
                tjenesteKode, 1, null)
            logger.info { "REGISTER size ${register.getRightResponse.size}" }
            val result = mutableListOf<RegistryResponse.Register>()
            register.getRightResponse.forEach {
                logger.info { "reportee: ${it.reportee} og condition: ${it.condition}" }
                result.add(RegistryResponse.Register(it.reportee, it.condition, it.right.toString(), it.validTo.toString()))
            }
            return RightsResponse("Ok", RegistryResponse(result).toString())
        } catch (e: IRegisterSRRAgencyExternalBasicGetRightsBasicAltinnFaultFaultFaultMessage) {
            logger.error { "IRegisterSRRAgencyExternalBasic.getRightsBasic feilet \n" +
                        "\n ErrorMessage  ${e.faultInfo.altinnErrorMessage}" +
                        "\n ExtendedErrorMessage  ${e.faultInfo.altinnExtendedErrorMessage}" +
                        "\n LocalizedErrorMessage  ${e.faultInfo.altinnLocalizedErrorMessage}" +
                        "\n ErrorGuid  ${e.faultInfo.errorGuid}" +
                        "\n UserGuid  ${e.faultInfo.userGuid}" +
                        "\n UserId  ${e.faultInfo.userId}"
            }
        }
        return RightsResponse("Failed", "Unknown error occurred when getting rights registry, check logger")
    }

    fun getRightsForABusiness(tjenesteKode: String, reportee: String): RightsResponse {
        try {
            val register = env.mock.srrGetResponse ?: iRegisterSRRAgencyExternalBasic.getRightsBasic(altinnUsername, altinnUserPassword,
                    tjenesteKode, 1, reportee)
            val result = mutableListOf<RegistryResponse.Register>()
            register.getRightResponse.forEach {
                result.add(RegistryResponse.Register(it.reportee, it.condition, it.right.toString(), it.validTo.toString()))
            }

            return RightsResponse("Ok", "{\n" + RegistryResponse(result).toString() + "<br>}")
        } catch (e: IRegisterSRRAgencyExternalBasicGetRightsBasicAltinnFaultFaultFaultMessage) {
            logger.error {
                "IRegisterSRRAgencyExternalBasic.getRightsBasic feilet \n" +
                        "\n ErrorMessage  ${e.faultInfo.altinnErrorMessage}" +
                        "\n ExtendedErrorMessage  ${e.faultInfo.altinnExtendedErrorMessage}" +
                        "\n LocalizedErrorMessage  ${e.faultInfo.altinnLocalizedErrorMessage}" +
                        "\n ErrorGuid  ${e.faultInfo.errorGuid}" +
                        "\n UserGuid  ${e.faultInfo.userGuid}" +
                        "\n UserId  ${e.faultInfo.userId}"
            }
        }

        return RightsResponse("Failed", "Unknown error occurred when getting rights registry, check logger")
    }

    private fun createAddRightsList(orgNr: String, domain: String, type: RegisterSRRRightsType): AddRightRequestList {

        return AddRightRequestList().apply {
            addRightRequest.add(
                    AddRightRequest().apply {
                        condition = "ALLOWEDREDIRECTDOMAIN:$domain"
                        reportee = orgNr
                        right = type
                        validTo = DatatypeFactory.newInstance().newXMLGregorianCalendar(GregorianCalendar.from(ZonedDateTime.now().plusYears(2)))
                    }
            )
        }
    }

    private fun createDeleteRightsList(orgNr: String, domain: String, type: RegisterSRRRightsType): DeleteRightRequestList {

        return DeleteRightRequestList().apply {
            deleteRightRequest.add(
                    DeleteRightRequest().apply {
                        condition = "ALLOWEDREDIRECTDOMAIN:$domain"
                        reportee = orgNr
                        right = type
                    }
            )
        }
    }

    private fun createAddResponseMessage(response: AddRightResponseList, type: RegisterSRRRightsType, reportee: String): RightsResponse {
        if (response.addRightResponse.size != 1) {
            logger.error { "Adding $type rights failed for business number $reportee." }
            return RightsResponse("Failed", "Expected only one AddRightsResponse in list, size is " +
                    response.addRightResponse.size)
        }
        val rRespond = response.addRightResponse[0]
        return if (rRespond.operationResult == OperationResult.OK) {
            logger.info { "Add $type rights for business number $reportee went ok." }
            RightsResponse("Ok", "Ok")
        } else {
            logger.error { "Failed to add $type rights for business number $reportee, error is ${rRespond.operationResult}." }
            RightsResponse("Failed", "Add operation failed with " + rRespond.operationResult)
        }
    }

    private fun createDeleteResponseMessage(response: DeleteRightResponseList, type: RegisterSRRRightsType, reportee: String): RightsResponse {
        if (response.deleteRightResponse.size != 1) {
            logger.error { "Removing read rights failed for business number $reportee." }
            return RightsResponse("Failed", "Expected only one DeleteRightsResponse in list, size is " +
                    response.deleteRightResponse.size)
        }
        val rRespond = response.deleteRightResponse[0]
        return if (rRespond.operationResult == OperationResult.OK) {
            logger.info { "Removing $type rights for business number $reportee went ok." }
            RightsResponse("Ok", "Ok")
        } else {
            logger.error { "Failed to remove read rights for business number $reportee, error is ${rRespond.operationResult}." }
            RightsResponse("Failed", "Remove operation failed with " + rRespond.operationResult)
        }
    }
}
