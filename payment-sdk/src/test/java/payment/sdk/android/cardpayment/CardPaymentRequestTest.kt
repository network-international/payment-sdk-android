package payment.sdk.android.cardpayment

import com.flextrade.jfixture.FixtureAnnotations
import com.flextrade.jfixture.JFixture
import com.flextrade.jfixture.annotations.Fixture
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test


class CardPaymentRequestTest {

    @Fixture
    private lateinit var fixtGatewayUrl: String

    @Fixture
    private lateinit var code: String

    private val fixture = JFixture()

    @Before
    fun setup() {
        FixtureAnnotations.initFixtures(this, fixture)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test builder when no url set`() {
        CardPaymentRequest.Builder()
                .build()
    }

    @Test()
    fun `test builder when all set`() {
        val request = CardPaymentRequest.Builder()
                .gatewayUrl(fixtGatewayUrl)
                .code(code)
                .build()
        assertThat(request, notNullValue())
    }

}