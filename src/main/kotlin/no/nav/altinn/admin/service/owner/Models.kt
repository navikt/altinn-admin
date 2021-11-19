package no.nav.altinn.admin.service.owner

import com.fasterxml.jackson.annotation.JsonProperty

data class ReporteeList(
    @JsonProperty("reportees")
    val reportees: List<Any>
)

open class Reportee(
    @JsonProperty("Name")
    val name: String,
    @JsonProperty("Type")
    val type: ReporteeType,
    @JsonProperty("SocialSecurityNumber")
    val socialsecuritynumber: String?,
    @JsonProperty("OrganizationNumber")
    val organizationnumber: String?,
    @JsonProperty("ParentOrganizationNumber")
    val parentorganizationnumber: String?,
    @JsonProperty("OrganizationForm")
    val organizationform: String?,
    @JsonProperty("Status")
    val status: String?
)

data class RightsRespons(
    @JsonProperty("Subject")
    val subject: Reportee,
    @JsonProperty("Reportee")
    val reportee: Reportee,
    @JsonProperty("Rights")
    val rights: List<Right>
)

data class Right(
    @JsonProperty("RightID")
    val rightId: Int,
    @JsonProperty("RightType")
    val rightType: String,
    @JsonProperty("ServiceCode")
    val serviceCode: String,
    @JsonProperty("ServiceEditionCode")
    val serviceEditionCode: Int,
    @JsonProperty("Action")
    val action: String,
    @JsonProperty("RightSourceType")
    val rightSourceType: String,
    @JsonProperty("IsDelegatable")
    val isDelegatable: Boolean
)

enum class ReporteeType {
    Enterprise,
    Person,
    Business
}
