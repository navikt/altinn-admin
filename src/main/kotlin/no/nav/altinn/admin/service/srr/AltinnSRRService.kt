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
import no.nav.altinn.admin.metrics.Metrics
import javax.xml.datatype.DatatypeFactory
import java.time.ZonedDateTime
import java.util.*

private val logger = KotlinLogging.logger { }

class AltinnSRRService(private val env: Environment, iRegisterSRRAgencyExternalBasicFactory: () -> IRegisterSRRAgencyExternalBasic) {

    private val altinnUsername = env.altinn.username
    private val altinnUserPassword = env.altinn.password
    private val iRegisterSRRAgencyExternalBasic: IRegisterSRRAgencyExternalBasic by lazy(iRegisterSRRAgencyExternalBasicFactory)

    fun addRights(serviceCode: String, editionCode: String, reportee: String, redirectDomain: String, type: RegisterSRRRightsType): RightsResponse {
        logger.info { "Adding $type rights for business number $reportee with redirect url $redirectDomain." }
        try {
            Metrics.addRightsRequest.labels(serviceCode).inc()
            val response = env.mock.srrAddResponse ?: iRegisterSRRAgencyExternalBasic.addRightsBasic(altinnUsername, altinnUserPassword, serviceCode, editionCode.toInt(),
                    createAddRightsList(reportee, redirectDomain, type))

            return createAddResponseMessage(serviceCode, response, type, reportee)
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
        Metrics.addRightsFailed.labels(serviceCode).inc()
        return RightsResponse("Failed", "Unknown error occurred when adding $type rights, check logger")
    }

    fun deleteRights(serviceCode: String, editionCode: String, reportee: String, redirectDomain: String, type: RegisterSRRRightsType): RightsResponse {
        logger.info { "Removing read rights for business number $reportee with redirect url $redirectDomain." }
        try {
            Metrics.deleteRightsRequest.labels(serviceCode).inc()
            val response = env.mock.srrDeleteResponse ?: iRegisterSRRAgencyExternalBasic.deleteRightsBasic(altinnUsername, altinnUserPassword, serviceCode, editionCode.toInt(),
                    createDeleteRightsList(reportee, redirectDomain, type))
            return createDeleteResponseMessage(serviceCode, response, type, reportee)
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
        Metrics.deleteRightsFailed.labels(serviceCode).inc()
        return RightsResponse("Failed", "Unknown error occurred when removing $type rights, check logger")
    }

    fun getRightsForAllBusinesses(tjenesteKode: String, utgaveKode: String = "1"): RightsResponseWithList {
        try {
            Metrics.getRightsRequest.labels(tjenesteKode).inc()
            logger.debug { "Tries to get all righsts..." }
            val register = env.mock.srrGetResponse ?: iRegisterSRRAgencyExternalBasic.getRightsBasic(altinnUsername, altinnUserPassword,
                tjenesteKode, utgaveKode.toInt(), null)
            logger.debug { "REGISTER size ${register.getRightResponse.size}" }
            val result = mutableListOf<RegistryResponse.Register>()
            register.getRightResponse.forEach {
                logger.debug { "reportee: ${it.reportee} og condition: ${it.condition}" }
                result.add(RegistryResponse.Register("$tjenesteKode:$utgaveKode", it.reportee, it.condition, it.right.toString(), it.validTo.toString()))
            }
            Metrics.getRightsResponse.labels(tjenesteKode).inc()
            return RightsResponseWithList("Ok", "Ok", RegistryResponse(result))
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
        Metrics.getRightsFailed.labels(tjenesteKode).inc()
        return RightsResponseWithList("Failed", "Unknown error occurred when getting rights registry, check logger", RegistryResponse(emptyList()))
    }

    fun getRightsForABusiness(tjenesteKode: String, reportee: String, utgaveKode: String = "1"): RightsResponseWithList {
        try {
            val register = env.mock.srrGetResponse ?: iRegisterSRRAgencyExternalBasic.getRightsBasic(altinnUsername, altinnUserPassword,
                    tjenesteKode, utgaveKode.toInt(), reportee)
            val result = mutableListOf<RegistryResponse.Register>()
            register.getRightResponse.forEach {
                result.add(RegistryResponse.Register("$tjenesteKode:$utgaveKode", it.reportee, it.condition, it.right.toString(), it.validTo.toString()))
            }

            return RightsResponseWithList("Ok", "Ok", RegistryResponse(result))
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
        Metrics.getRightsFailed.labels(tjenesteKode).inc()
        return RightsResponseWithList("Failed", "Unknown error occurred when getting rights registry, check logger", RegistryResponse(emptyList()))
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

    private fun createAddResponseMessage(sc: String, response: AddRightResponseList, type: RegisterSRRRightsType, reportee: String): RightsResponse {
        if (response.addRightResponse.size != 1) {
            logger.error { "Adding $type rights failed for business number $reportee." }
            Metrics.addRightsFailed.labels(sc).inc()
            return RightsResponse("Failed", "Expected only one AddRightsResponse in list, size is " +
                    response.addRightResponse.size)
        }
        val rRespond = response.addRightResponse[0]
        return if (rRespond.operationResult == OperationResult.OK) {
            logger.info { "Add $type rights for business number $reportee went ok." }
            Metrics.addRightsResponse.labels(sc).inc()
            RightsResponse("Ok", "Ok")
        } else {
            logger.error { "Failed to add $type rights for business number $reportee, error is ${rRespond.operationResult}." }
            Metrics.addRightsFailed.labels(sc).inc()
            RightsResponse("Failed", "Add operation failed with " + rRespond.operationResult)
        }
    }

    private fun createDeleteResponseMessage(sc: String, response: DeleteRightResponseList, type: RegisterSRRRightsType, reportee: String): RightsResponse {
        if (response.deleteRightResponse.size != 1) {
            Metrics.deleteRightsFailed.labels(sc).inc()
            logger.error { "Removing read rights failed for business number $reportee." }
            return RightsResponse("Failed", "Expected only one DeleteRightsResponse in list, size is " +
                    response.deleteRightResponse.size)
        }
        val rRespond = response.deleteRightResponse[0]
        return if (rRespond.operationResult == OperationResult.OK) {
            logger.info { "Removing $type rights for business number $reportee went ok." }
            Metrics.deleteRightsResponse.labels(sc).inc()
            RightsResponse("Ok", "Ok")
        } else {
            logger.error { "Failed to remove read rights for business number $reportee, error is ${rRespond.operationResult}." }
            Metrics.deleteRightsFailed.labels(sc).inc()
            RightsResponse("Failed", "Remove operation failed with " + rRespond.operationResult)
        }
    }
}
