package no.nav.altinn.admin.service.dq

import com.fasterxml.jackson.annotation.JsonProperty

data class DqResponse(
    @JsonProperty("status")
    val status: String,
    @JsonProperty("message")
    val message: String
)

data class DqResponseFormData(
    @JsonProperty("status")
    val status: String,
    @JsonProperty("message")
    val message: String,
    @JsonProperty("formdata")
    val formdata: String
)
