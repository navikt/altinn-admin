package no.nav.altinn.admin

import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths

private object Utils

fun String.getResource(): String = String(
    Files.readAllBytes(Paths.get(Utils::class.java.getResource(this).toURI())),
    Charset.forName("UTF-8")
)
