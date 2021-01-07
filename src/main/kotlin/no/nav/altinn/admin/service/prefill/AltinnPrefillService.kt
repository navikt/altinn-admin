package no.nav.altinn.admin.service.prefill

import javax.xml.datatype.XMLGregorianCalendar
import mu.KotlinLogging
import no.altinn.schemas.services.serviceengine.prefill._2009._10.PrefillFormTask
import no.altinn.services.serviceengine.prefill._2009._10.IPreFillExternalBasic
import no.altinn.services.serviceengine.prefill._2009._10.IPreFillExternalBasicSubmitAndInstantiatePrefilledFormTaskBasicAltinnFaultFaultFaultMessage
import no.nav.altinn.admin.Environment
import no.nav.altinn.admin.common.randomUuid

private val logger = KotlinLogging.logger { }

class AltinnPrefillService(env: Environment, iPrefillExternalBasicV2Factory: () -> IPreFillExternalBasic) {
    private val altinnUsername = env.altinn.username
    private val altinnUserPassword = env.altinn.password
    private val iPrefillExternalBasic by lazy(iPrefillExternalBasicV2Factory)

    fun prefillFormTask(prefillFormTask: PrefillFormTask, dueDate: XMLGregorianCalendar): PrefillResponse {
        val receiptExternal = try {
            iPrefillExternalBasic.submitAndInstantiatePrefilledFormTaskBasic(
                altinnUsername,
                altinnUserPassword,
                randomUuid(),
                prefillFormTask,
                false,
                true,
                null,
                dueDate
            )
        } catch (e: IPreFillExternalBasicSubmitAndInstantiatePrefilledFormTaskBasicAltinnFaultFaultFaultMessage) {
            logger.warn { "Exception when sending prefill ${e.faultInfo.altinnErrorMessage}" }
            logger.warn { e.faultInfo.altinnExtendedErrorMessage }
            throw RuntimeException(
                "SubmitAndInstantiatePrefilledFormTask feilet" +
                    "\n ErrorMessage  ${e.faultInfo.altinnErrorMessage}" +
                    "\n ExtendedErrorMessage  ${e.faultInfo.altinnExtendedErrorMessage}" +
                    "\n LocalizedErrorMessage  ${e.faultInfo.altinnLocalizedErrorMessage}" +
                    "\n ErrorGuid  ${e.faultInfo.errorGuid}" +
                    "\n UserGuid  ${e.faultInfo.userGuid}" +
                    "\n UserId  ${e.faultInfo.userId}",
                e
            )
        } catch (e: Throwable) {
            logger.warn { "RuntimeException ${e.printStackTrace()}" }
            throw RuntimeException("AltinnPrefillService - Opprettelse av Altinnskjema feilet", e)
        }
        return PrefillResponse(receiptExternal.receiptStatusCode.value(), receiptExternal.receiptText)
    }
}
