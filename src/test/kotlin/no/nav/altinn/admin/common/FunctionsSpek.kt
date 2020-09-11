package no.nav.altinn.admin.common

import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.test.assertFalse
import kotlin.test.assertTrue

object FunctionsSpek : Spek({
    describe("Testing of date and dateTime regex") {
        it("Quick testing") {
            var expectedDate = isDateTime("2020-01-01T00:30:12")
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
})