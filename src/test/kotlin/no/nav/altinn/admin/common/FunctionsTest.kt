package no.nav.altinn.admin.common

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FunctionsTest {

    @Test
    fun testDate() {
        var expectedDate = isDateTime("2020-01-01 00:30:12")
        assertTrue { expectedDate }

        expectedDate = isDate("2020-01-01")
        assertTrue { expectedDate }

        expectedDate = isDateTime("2020-01-0100:30:12")
        assertFalse { expectedDate }

        expectedDate = isDate("020-01-01")
        assertFalse { expectedDate }

        expectedDate = isDate("2020-01-0|")
        assertFalse { expectedDate }
    }
}
