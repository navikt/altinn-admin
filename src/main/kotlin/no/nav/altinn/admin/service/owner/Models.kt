package no.nav.altinn.admin.service.owner

import com.fasterxml.jackson.annotation.JsonProperty

data class ReporteeList(
    @JsonProperty("reportees")
    val reportees: List<Dummy>
)

data class Dummy(
    @JsonProperty("reportee")
    val reportee: String
)
