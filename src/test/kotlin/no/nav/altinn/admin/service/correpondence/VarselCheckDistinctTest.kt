package no.nav.altinn.admin.service.correpondence

import java.time.LocalDateTime
import kotlin.test.Test
import no.nav.altinn.admin.service.correspondence.ForsendelseType
import no.nav.altinn.admin.service.correspondence.Mottaker
import no.nav.altinn.admin.service.correspondence.Varsel
import no.nav.altinn.admin.service.correspondence.VarselType
import org.amshove.kluent.`should be equal to`

class VarselCheckDistinctTest {

    @Test
    fun `Varsel Tittel sjekk - Har en lik tittel`() {
        val varsler = mutableListOf<Varsel>()
        val ekstraMottaker = mutableListOf<Mottaker>()
        ekstraMottaker.add(Mottaker(ForsendelseType.Email, "alf@nav.com"))
        val ekstraMottaker2 = mutableListOf<Mottaker>()
        ekstraMottaker2.add(Mottaker(ForsendelseType.Email, "alf2@nav.com"))
        varsler.add(
            Varsel(
                "ikke-besvar-denne@nav.no",
                LocalDateTime.now().plusMinutes(10),
                VarselType.TokenTextOnly,
                "Tittel i varsel",
                "Innhold i varsel",
                ekstraMottaker
            )
        )
        varsler.add(
            Varsel(
                "ikke-besvar-denne@nav.no",
                LocalDateTime.now().plusMinutes(10),
                VarselType.TokenTextOnly,
                "Tittel i varsel 1",
                "Innhold i varsel",
                ekstraMottaker
            )
        )
        varsler.add(
            Varsel(
                "ikke-besvar-denne@nav.no",
                LocalDateTime.now().plusMinutes(10),
                VarselType.TokenTextOnly,
                "Tittel i varsel",
                "Innhold i varsel",
                ekstraMottaker
            )
        )
        val remove =
            varsler.distinctBy { it.tittel to it.melding to it.ekstraMottakere[0].forsendelseType to it.ekstraMottakere[0].mottakerAdresse }
                .toList()
        remove.size `should be equal to` 2
    }

    @Test
    fun `Varsel Melding sjekk - Har en lik melding`() {
        val varsler = mutableListOf<Varsel>()
        val ekstraMottaker = mutableListOf<Mottaker>()
        ekstraMottaker.add(Mottaker(ForsendelseType.Email, "alf@nav.com"))
        val ekstraMottaker2 = mutableListOf<Mottaker>()
        ekstraMottaker2.add(Mottaker(ForsendelseType.Email, "alf2@nav.com"))
        varsler.add(
            Varsel(
                "ikke-besvar-denne@nav.no",
                LocalDateTime.now().plusMinutes(10),
                VarselType.TokenTextOnly,
                "Tittel i varsel",
                "Innhold i varsel",
                ekstraMottaker
            )
        )
        varsler.add(
            Varsel(
                "ikke-besvar-denne@nav.no",
                LocalDateTime.now().plusMinutes(10),
                VarselType.TokenTextOnly,
                "Tittel i varsel",
                "Innhold i varsel ulik",
                ekstraMottaker
            )
        )
        varsler.add(
            Varsel(
                "ikke-besvar-denne@nav.no",
                LocalDateTime.now().plusMinutes(10),
                VarselType.TokenTextOnly,
                "Tittel i varsel",
                "Innhold i varsel",
                ekstraMottaker
            )
        )
        val remove =
            varsler.distinctBy { it.tittel to it.melding to it.ekstraMottakere[0].forsendelseType to it.ekstraMottakere[0].mottakerAdresse }
                .toList()
        remove.size `should be equal to` 2
    }

    @Test
    fun `Varsel Mottaker sjekk - Har en lik spesifisert mottaker epost adresse`() {
        val varsler = mutableListOf<Varsel>()
        val ekstraMottaker = mutableListOf<Mottaker>()
        ekstraMottaker.add(Mottaker(ForsendelseType.Email, "alf@nav.com"))
        val ekstraMottaker2 = mutableListOf<Mottaker>()
        ekstraMottaker2.add(Mottaker(ForsendelseType.Email, "alf2@nav.com"))
        varsler.add(
            Varsel(
                "ikke-besvar-denne@nav.no",
                LocalDateTime.now().plusMinutes(10),
                VarselType.TokenTextOnly,
                "Tittel i varsel",
                "Innhold i varsel",
                ekstraMottaker
            )
        )
        varsler.add(
            Varsel(
                "ikke-besvar-denne@nav.no",
                LocalDateTime.now().plusMinutes(10),
                VarselType.TokenTextOnly,
                "Tittel i varsel",
                "Innhold i varsel",
                ekstraMottaker2
            )
        )
        varsler.add(
            Varsel(
                "ikke-besvar-denne@nav.no",
                LocalDateTime.now().plusMinutes(10),
                VarselType.TokenTextOnly,
                "Tittel i varsel",
                "Innhold i varsel",
                ekstraMottaker
            )
        )
        val remove =
            varsler.distinctBy { it.tittel to it.melding to it.ekstraMottakere[0].forsendelseType to it.ekstraMottakere[0].mottakerAdresse }
                .toList()
        remove.size `should be equal to` 2
    }

    @Test
    fun `Varsel Mottaker sjekk - Har tre lik uspesifisert mottaker epost adresse`() {
        val varsler = mutableListOf<Varsel>()
        val ekstraMottaker = mutableListOf<Mottaker>()
        ekstraMottaker.add(Mottaker(ForsendelseType.Email, ""))
        val ekstraMottaker2 = mutableListOf<Mottaker>()
        ekstraMottaker2.add(Mottaker(ForsendelseType.Email, ""))
        varsler.add(
            Varsel(
                "ikke-besvar-denne@nav.no",
                LocalDateTime.now().plusMinutes(10),
                VarselType.TokenTextOnly,
                "Tittel i varsel",
                "Innhold i varsel",
                ekstraMottaker
            )
        )
        varsler.add(
            Varsel(
                "ikke-besvar-denne@nav.no",
                LocalDateTime.now().plusMinutes(10),
                VarselType.TokenTextOnly,
                "Tittel i varsel",
                "Innhold i varsel",
                ekstraMottaker2
            )
        )
        varsler.add(
            Varsel(
                "ikke-besvar-denne@nav.no",
                LocalDateTime.now().plusMinutes(10),
                VarselType.TokenTextOnly,
                "Tittel i varsel",
                "Innhold i varsel",
                ekstraMottaker
            )
        )
        val remove =
            varsler.distinctBy { it.tittel to it.melding to it.ekstraMottakere[0].forsendelseType to it.ekstraMottakere[0].mottakerAdresse }
                .toList()
        remove.size `should be equal to` 1
    }

    @Test
    fun `Varsel Mottaker sjekk - Har en lik forsendelsestype i mottaker`() {
        val varsler = mutableListOf<Varsel>()
        val ekstraMottaker = mutableListOf<Mottaker>()
        ekstraMottaker.add(Mottaker(ForsendelseType.Email, "alf@nav.com"))
        val ekstraMottaker2 = mutableListOf<Mottaker>()
        ekstraMottaker2.add(Mottaker(ForsendelseType.Both, "alf@nav.com"))
        varsler.add(
            Varsel(
                "ikke-besvar-denne@nav.no",
                LocalDateTime.now().plusMinutes(10),
                VarselType.TokenTextOnly,
                "Tittel i varsel",
                "Innhold i varsel",
                ekstraMottaker
            )
        )
        varsler.add(
            Varsel(
                "ikke-besvar-denne@nav.no",
                LocalDateTime.now().plusMinutes(10),
                VarselType.TokenTextOnly,
                "Tittel i varsel",
                "Innhold i varsel",
                ekstraMottaker2
            )
        )
        varsler.add(
            Varsel(
                "ikke-besvar-denne@nav.no",
                LocalDateTime.now().plusMinutes(10),
                VarselType.TokenTextOnly,
                "Tittel i varsel",
                "Innhold i varsel",
                ekstraMottaker
            )
        )
        val remove =
            varsler.distinctBy { it.tittel to it.melding to it.ekstraMottakere[0].forsendelseType to it.ekstraMottakere[0].mottakerAdresse }
                .toList()
        remove.size `should be equal to` 2
    }
}
