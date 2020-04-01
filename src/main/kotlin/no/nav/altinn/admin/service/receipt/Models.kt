package no.nav.altinn.admin.service.receipt

import com.fasterxml.jackson.annotation.JsonProperty

data class ReceiptItems(
    @JsonProperty("status")
    val status: String,
    @JsonProperty("size")
    val size: Int,
    @JsonProperty("arReceipts")
    val arReceipts: List<String>
)

data class CorrespondenceReceiptItems(
    @JsonProperty("status")
    val status: String,
    @JsonProperty("size")
    val size: Int,
    @JsonProperty("receipts")
    val receipts: List<CorrespondenceReceipt>
)

data class CorrespondenceReceipt(
    @JsonProperty("receiptId")
    val receiptId: String,
    @JsonProperty("externalShipmentReference")
    val externalShipmentReference: String
)