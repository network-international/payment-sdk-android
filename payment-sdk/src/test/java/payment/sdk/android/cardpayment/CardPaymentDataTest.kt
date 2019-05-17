package payment.sdk.android.cardpayment


import android.content.Intent
import com.flextrade.jfixture.FixtureAnnotations
import com.flextrade.jfixture.annotations.Fixture
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.lang.IllegalArgumentException

class CardPaymentDataTest {

    @Mock
    lateinit var mockIntent: Intent

    @Fixture
    lateinit var fixtCardData: CardPaymentData


    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        FixtureAnnotations.initFixtures(this)
    }

    @Test
    fun getFromIntent() {
        whenever(mockIntent.getParcelableExtra<CardPaymentData>(CardPaymentData.INTENT_DATA_KEY))
                .thenReturn(fixtCardData)

        val actual = CardPaymentData.getFromIntent(mockIntent)

        assertEquals(fixtCardData, actual)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `getFromIntent when intent key not matched`() {
        whenever(mockIntent.getParcelableExtra<CardPaymentData>(CardPaymentData.INTENT_DATA_KEY))
                .thenThrow(IllegalArgumentException())

        CardPaymentData.getFromIntent(mockIntent)
    }
}