package payment.sdk.android.visaInstalments.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import payment.sdk.android.core.CostInfo
import payment.sdk.android.core.LastInstallment
import payment.sdk.android.core.MatchedPlan
import payment.sdk.android.core.OrderAmount
import payment.sdk.android.core.VisaPlans

/**
 * `fromVisaPlans` turns the gateway's plan list into the pills the instalment screen renders.
 * Robolectric because it reads the layout direction through `TextUtilsCompat`.
 */
@RunWith(RobolectricTestRunner::class)
class InstallmentPlanTest {

    private fun plan(
        frequency: String,
        vPlanID: String = "plan-1",
        installments: Int = 6
    ) = MatchedPlan(
        costInfo = CostInfo(
            annualPercentageRate = 280.0,
            currency = "AED",
            lastInstallment = LastInstallment(
                amount = 10800.0, installmentFee = 0.0,
                totalAmount = 10800.0, upfrontFee = 0.0
            ),
            totalFees = 0.0,
            totalPlanCost = 10800.0,
            totalRecurringFees = 0.0,
            totalUpfrontFees = 2000.0
        ),
        fundedBy = listOf("BANK"),
        installmentFrequency = frequency,
        name = "Plan $frequency",
        numberOfInstallments = installments,
        termsAndConditions = emptyList(),
        type = "ISSUER",
        vPlanID = vPlanID,
        vPlanIDRef = "$vPlanID-ref"
    )

    private fun build(vararg frequencies: String) = InstallmentPlan.fromVisaPlans(
        VisaPlans(matchedPlans = frequencies.map { plan(it) }),
        OrderAmount(56560.0, "AED")
    )

    @Test
    fun `each gateway frequency maps to its own plan frequency`() {
        // A mapping that collapsed every value to MONTHLY went unnoticed before this test.
        assertEquals(PlanFrequency.MONTHLY, build("MONTHLY")[1].frequency)
        assertEquals(PlanFrequency.WEEKLY, build("WEEKLY")[1].frequency)
        assertEquals(PlanFrequency.BI_MONTHLY, build("BIMONTHLY")[1].frequency)
        assertEquals(PlanFrequency.BI_WEEKLY, build("BIWEEKLY")[1].frequency)
    }

    @Test
    fun `an unrecognised frequency falls back to pay in full`() {
        assertEquals(PlanFrequency.PayInFull, build("FORTNIGHTLY")[1].frequency)
    }

    @Test
    fun `pay in full is offered first, ahead of the gateway's plans`() {
        val plans = build("MONTHLY", "WEEKLY")

        assertEquals(3, plans.size)
        assertEquals(InstallmentPlan.PAY_IN_FULL_ID, plans.first().id)
        assertEquals(PlanFrequency.PayInFull, plans.first().frequency)
        assertEquals(0, plans.first().numberOfInstallments)
    }

    @Test
    fun `the pay-in-full id is stable across rebuilds`() {
        // It used to be a fresh UUID per call, so the selected pill stopped matching itself on
        // recomposition and appeared unselected. Nothing asserted the fix until now.
        assertEquals(build("MONTHLY").first().id, build("MONTHLY").first().id)
        assertEquals(InstallmentPlan.PAY_IN_FULL_ID, build("MONTHLY").first().id)
        // Pinned as a literal too: comparing against the constant alone still passes if the
        // constant itself is swapped for something generated.
        assertEquals("pay-in-full", build("MONTHLY").first().id)
    }

    @Test
    fun `pay in full shows the order amount, not a plan amount`() {
        val plans = build("MONTHLY")

        assertTrue(plans.first().amount.contains("565.60"))
        assertNotEquals(plans.first().amount, plans[1].amount)
    }

    @Test
    fun `a remote plan carries its id, instalment count and monthly rate`() {
        val remote = build("MONTHLY")[1]

        assertEquals("plan-1", remote.id)
        assertEquals(6, remote.numberOfInstallments)
        assertEquals("AED", remote.currency)
        // 280.0 APR is rendered as a per-annum percentage divided by 100.
        assertEquals("2.80", remote.monthlyRate)
    }

    @Test
    fun `an empty plan list still offers pay in full`() {
        val plans = InstallmentPlan.fromVisaPlans(
            VisaPlans(matchedPlans = emptyList()),
            OrderAmount(56560.0, "AED")
        )

        assertEquals(1, plans.size)
        assertEquals(InstallmentPlan.PAY_IN_FULL_ID, plans.first().id)
    }
}
