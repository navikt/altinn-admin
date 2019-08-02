package no.nav.altinn.admin.service.dq

import com.fasterxml.jackson.annotation.JsonProperty

data class DqResponseFormData(
    @JsonProperty("status")
    val status: String,
    @JsonProperty("message")
    val message: String,
    @JsonProperty("formData")
    val formData: FormData
)

data class DqItems(
    @JsonProperty("status")
    val status: String,
    @JsonProperty("items")
    val items: List<DqItem>
)

data class DqItem(
    @JsonProperty("archiveReference")
    val archiveReference: String
)

data class DqPurge(
    @JsonProperty("status")
    val status: String,
    @JsonProperty("message")
    val message: String
)

data class FormData(
    @JsonProperty("formData")
    val formData: String
)