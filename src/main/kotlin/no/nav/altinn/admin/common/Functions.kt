package no.nav.altinn.admin.common

import java.util.*

internal fun randomUuid() = UUID.randomUUID().toString()
internal fun decodeBase64(s: String): ByteArray = Base64.getDecoder().decode(s)
fun encodeBase64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
