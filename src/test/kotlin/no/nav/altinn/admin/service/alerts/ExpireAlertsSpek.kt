package no.nav.altinn.admin.service.alerts

import java.util.Calendar
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object ExpireAlertsSpek : Spek({

    describe("Testing expire date formatting") {
        it("Test 3 month expire date") {

            var expectedDate = Calendar.getInstance()
            expectedDate.add(Calendar.MONTH, 3)

            val actualDate = getRelativeExpireDate("0y3m0d")
            expectedDate.get(Calendar.DATE) shouldBeEqualTo actualDate.get(Calendar.DATE)
            expectedDate.get(Calendar.MONTH) shouldBeEqualTo actualDate.get(Calendar.MONTH)
            expectedDate.get(Calendar.YEAR) shouldBeEqualTo actualDate.get(Calendar.YEAR)

            val actualDate2 = getRelativeExpireDate("0y3M0d")
            expectedDate.get(Calendar.DATE) shouldBeEqualTo actualDate2.get(Calendar.DATE)
            expectedDate.get(Calendar.MONTH) shouldBeEqualTo actualDate2.get(Calendar.MONTH)
            expectedDate.get(Calendar.YEAR) shouldBeEqualTo actualDate2.get(Calendar.YEAR)
        }

        it("Test 13 month and 20 days expire date") {

            var expectedDate = Calendar.getInstance()
            expectedDate.add(Calendar.MONTH, 13)
            expectedDate.add(Calendar.DATE, 20)

            val actualDate = getRelativeExpireDate("0y13m20d")
            expectedDate.get(Calendar.DATE) shouldBeEqualTo actualDate.get(Calendar.DATE)
            expectedDate.get(Calendar.MONTH) shouldBeEqualTo actualDate.get(Calendar.MONTH)
            expectedDate.get(Calendar.YEAR) shouldBeEqualTo actualDate.get(Calendar.YEAR)
        }

        it("Wrong expire date format, goes to default of 2 months.") {

            var expectedDate = Calendar.getInstance()
            expectedDate.add(Calendar.MONTH, 2)

            val actualDate = getRelativeExpireDate("0y13F20d")
            expectedDate.get(Calendar.DATE) shouldBeEqualTo actualDate.get(Calendar.DATE)
            expectedDate.get(Calendar.MONTH) shouldBeEqualTo actualDate.get(Calendar.MONTH)
            expectedDate.get(Calendar.YEAR) shouldBeEqualTo actualDate.get(Calendar.YEAR)
        }
    }
})
