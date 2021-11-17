package no.nav.altinn.admin.service.owner

import com.fasterxml.jackson.annotation.JsonProperty

data class ReporteeList(
    @JsonProperty("reportees")
    val reportees: List<Reportee>
)

data class Reportee(
    @JsonProperty("name")
    val name: String,
    @JsonProperty("type")
    val type: String,
    @JsonProperty("socialsecuritynumber")
    val socialsecuritynumber: String?,
    @JsonProperty("organizationnumber")
    val organizationnumber: String?,
    @JsonProperty("organizationform")
    val organizationform: String?,
    @JsonProperty("status")
    val status: String?
)
