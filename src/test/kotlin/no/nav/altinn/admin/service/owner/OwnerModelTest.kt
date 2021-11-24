package no.nav.altinn.admin.service.owner

import com.fasterxml.jackson.module.kotlin.readValue
import kotlin.test.Test
import no.nav.altinn.admin.common.objectMapper
import no.nav.altinn.admin.getResource
import no.nav.altinn.admin.service.login.UserInfo
import org.amshove.kluent.`should be equal to`

class OwnerModelTest {
    val reporteeRespons = "/reportee_response.json".getResource()
    val rightsRespons = "/rights_response.json".getResource()
    val srrResponse = "/srr_response.json".getResource()
    val userInfoResponse = "/user_info_response.json".getResource()

    @Test
    fun testReporteesRespons() {
        val reportees = objectMapper.readValue<List<Reportee>>(reporteeRespons)
        reportees.size `should be equal to` 50
    }

    @Test
    fun testRightsRespons() {
        val rights = objectMapper.readValue<RightsResponse>(rightsRespons)
        rights.rights.size `should be equal to` 50
    }

    @Test
    fun testSrrResponse() {
        val srrResponse = objectMapper.readValue<List<SrrResponse>>(srrResponse)
        srrResponse.size `should be equal to` 5
    }

    @Test
    fun testUserInfoResponse() {
        val userInfo = objectMapper.readValue<UserInfo>(userInfoResponse)
        userInfo.given_name `should be equal to` "Nordmann"
    }
}
