package no.nav.altinn.admin.service.owner

import com.fasterxml.jackson.module.kotlin.readValue
import kotlin.test.Test
import no.nav.altinn.admin.common.objectMapper
import no.nav.altinn.admin.getResource
import org.amshove.kluent.`should be equal to`

class OwnerModelTest {
    val reporteeRespons = "/reportee_response.json".getResource()
    val rightsRespons = "/rights_response.json".getResource()

    @Test
    fun testReporteesRespons() {
        val reportees = objectMapper.readValue<List<Reportee>>(reporteeRespons)
        reportees.size `should be equal to` 50
    }

    @Test
    fun testRightsRespons() {
        val rights = objectMapper.readValue<RightsRespons>(rightsRespons)
        rights.rights.size `should be equal to` 50
    }
}
