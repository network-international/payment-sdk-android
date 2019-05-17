package payment.sdk.android.cardpayment.threedsecure

import android.app.Activity
import android.content.Intent
import com.flextrade.jfixture.FixtureAnnotations
import com.flextrade.jfixture.annotations.Fixture
import com.nhaarman.mockitokotlin2.*
import org.junit.Assert.assertEquals
import org.junit.Test

import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment


@RunWith(RobolectricTestRunner::class)
class ThreeDSecureWebViewClientTest {

    @Fixture
    private lateinit var fixtAcsPaReq: String

    @Fixture
    private lateinit var fixtAcsUrl: String

    @Fixture
    private lateinit var fixtAcsMd: String

    @Fixture
    private lateinit var fixtGatewayUrl: String

    @Fixture
    private lateinit var fixtState: String

    private lateinit var sut: ThreeDSecureWebViewClient
    private lateinit var spiedActivity: ThreeDSecureWebViewActivity

    @Before
    fun setUp() {
        FixtureAnnotations.initFixtures(this)
        val intent = ThreeDSecureWebViewActivity.getIntent(
                context = RuntimeEnvironment.application,
                acsPaReq = fixtAcsPaReq,
                acsUrl = fixtAcsUrl,
                acsMd = fixtAcsMd,
                gatewayUrl = fixtGatewayUrl)

        spiedActivity = spy(Robolectric.buildActivity(
                ThreeDSecureWebViewActivity::class.java, intent).setup().get())
        sut = ThreeDSecureWebViewClient(spiedActivity)
    }

    @Test
    fun `onPageStarted when state CAPTURED`() {
        sut.onPageStarted(null, "http://server/?3ds_status=X&state=CAPTURED", null)

        val intentCaptor = argumentCaptor<Intent>()
        verify(spiedActivity).setResult(eq(Activity.RESULT_OK), intentCaptor.capture())

        val actual = intentCaptor.firstValue.getStringExtra(ThreeDSecureWebViewActivity.KEY_3DS_STATE)

        assertEquals("CAPTURED", actual)
    }

    @Test
    fun `onPageStarted when state AUTHORISED`() {
        sut.onPageStarted(null, "http://server/?3ds_status=X&state=AUTHORISED", null)

        val intentCaptor = argumentCaptor<Intent>()
        verify(spiedActivity).setResult(eq(Activity.RESULT_OK), intentCaptor.capture())

        val actual = intentCaptor.firstValue.getStringExtra(ThreeDSecureWebViewActivity.KEY_3DS_STATE)

        assertEquals("AUTHORISED", actual)
    }

    @Test
    fun `onPageStarted when state FAILED`() {
        sut.onPageStarted(null, "http://server/?3ds_status=X&state=FAILED", null)

        val intentCaptor = argumentCaptor<Intent>()
        verify(spiedActivity).setResult(eq(Activity.RESULT_OK), intentCaptor.capture())

        val actual = intentCaptor.firstValue.getStringExtra(ThreeDSecureWebViewActivity.KEY_3DS_STATE)

        assertEquals("FAILED", actual)
    }

    @Test
    fun `onPageStarted when state an invalid value`() {
        sut.onPageStarted(null, "http://server/?3ds_status=X&state=$fixtState", null)

        val intentCaptor = argumentCaptor<Intent>()
        verify(spiedActivity).setResult(eq(Activity.RESULT_OK), intentCaptor.capture())

        val actual = intentCaptor.firstValue.getStringExtra(ThreeDSecureWebViewActivity.KEY_3DS_STATE)

        assertEquals(fixtState, actual)
    }

    @Test
    fun `onPageStarted when 3ds status absent`() {
        sut.onPageStarted(null, "http://server/?state=$fixtState", null)

        verify(spiedActivity, never()).setResult(eq(Activity.RESULT_OK), any())
    }
}