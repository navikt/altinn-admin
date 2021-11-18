package no.nav.altinn.admin.service.owner

import com.fasterxml.jackson.annotation.JsonProperty

data class ReporteeList(
    @JsonProperty("reportees")
    val reportees: List<Any>
)

open class Reportee(
    @JsonProperty("name")
    val name: String,
    @JsonProperty("type")
    val type: ReporteeType
)

data class PersonReportee(
    @JsonProperty("name")
    val name: String,
    @JsonProperty("type")
    val type: ReporteeType = ReporteeType.PERSON,
    @JsonProperty("socialsecuritynumber")
    val socialsecuritynumber: String
)

data class EnterpriseReportee(
    @JsonProperty("name")
    val name: String,
    @JsonProperty("type")
    val type: ReporteeType = ReporteeType.ENTERPRISE,
    @JsonProperty("organizationnumber")
    val organizationnumber: String?,
    @JsonProperty("organizationform")
    val organizationform: String?,
    @JsonProperty("status")
    val status: String?
)

data class BusinessReportee(
    @JsonProperty("name")
    val name: String,
    @JsonProperty("type")
    val type: ReporteeType = ReporteeType.BUSINESS,
    @JsonProperty("organizationnumber")
    val organizationnumber: String?,
    @JsonProperty("parentorganizationnumber")
    val parentorganizationnumber: String?,
    @JsonProperty("organizationform")
    val organizationform: String?,
    @JsonProperty("status")
    val status: String?
)

enum class ReporteeType(val type: String) {
    ENTERPRISE("Enterprise"),
    PERSON("Person"),
    BUSINESS("Business")
}
