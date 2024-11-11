package payment.sdk.android.cardpayment.googlePay

import com.google.android.gms.wallet.PaymentsClient
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import payment.sdk.android.cardpayment.TestUtils
import payment.sdk.android.core.GooglePayConfigResponse
import payment.sdk.android.core.MerchantInfo
import payment.sdk.android.core.interactor.GooglePayConfigInteractor
import payment.sdk.android.googlepay.GooglePayConfigFactory
import payment.sdk.android.googlepay.GooglePayJsonConfig

@OptIn(ExperimentalCoroutinesApi::class)
internal class GooglePayUiConfigFactoryTest {

    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    private val paymentsClient: PaymentsClient = mockk(relaxed = true)

    private val googlePayJsonConfig: GooglePayJsonConfig = mockk(relaxed = true)

    private val googlePayConfigInteractor: GooglePayConfigInteractor = mockk(relaxed = true)

    lateinit var sut: GooglePayConfigFactory

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sut =
            GooglePayConfigFactory(paymentsClient, googlePayJsonConfig, googlePayConfigInteractor)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `checkGooglePayConfig should return GooglePayConfig when successful`() =
        runTest(testDispatcher) {
            coEvery {
                googlePayConfigInteractor.getConfig(
                    any(),
                    any()
                )
            } returns googlePayConfigResponse


            every { googlePayJsonConfig.create(any(), any(), any()) } returns paymentDataRequestJson

            coEvery {
                googlePayJsonConfig.baseCardPaymentMethod(any(), any())
            } returns JSONObject()

            coEvery { paymentsClient.isReadyToPay(any()) } returns TestUtils.mockTask<Boolean>(value = true)

            // When
            val result = sut.checkGooglePayConfig(googlePayConfigUrl, accessToken, 0.0, "AED", "acceptUrl")

            // Then
            assertNotNull(result)
            assertEquals(true, result?.canUseGooglePay)
        }

    @Test
    fun `checkGooglePayConfig should return null when config is null`() =
        runTest(testDispatcher) {
            // Given
            val googlePayConfigUrl = "https://config.url"
            val accessToken = "testAccessToken"

            coEvery {
                googlePayConfigInteractor.getConfig(
                    googlePayConfigUrl,
                    accessToken
                )
            } returns null

            // When
            val result = sut.checkGooglePayConfig(googlePayConfigUrl, accessToken, 0.0, "AED", "acceptUrl")

            // Then
            assertNull(result)
        }

    companion object {
        val googlePayConfigUrl = "https://config.url"
        val accessToken = "testAccessToken"
        val merchantInfo = MerchantInfo(
            name = "Test Merchant",
            reference = "12345"
        )
        val googlePayConfigResponse = GooglePayConfigResponse(
            allowedAuthMethods = listOf("PAN_ONLY", "CRYPTOGRAM_3DS"),
            allowedPaymentMethods = listOf("CARD", "TOKENIZED_CARD"),
            environment = "TEST",
            gatewayName = "gateway",
            merchantInfo = merchantInfo,
            merchantGatewayId = "merchantGatewayId",
            isMerchantCertificatePresent = true
        )
        val paymentDataRequestJson = """
                {
                  "apiVersion": 2,
                  "apiVersionMinor": 0,
                  "allowedPaymentMethods": [
                    {
                      "type": "CARD",
                      "parameters": {
                        "allowedAuthMethods": [
                          "PAN_ONLY",
                          "CRYPTOGRAM_3DS"
                        ],
                        "allowedCardNetworks": [
                          "VISA",
                          "MASTERCARD"
                        ]
                      },
                      "tokenizationSpecification": {
                        "type": "PAYMENT_GATEWAY",
                        "parameters": {
                          "gateway": "networkintl",
                          "gatewayMerchantId": "a9c12627-a429-49c1-b580-a938c86fd9c7"
                        }
                      }
                    }
                  ],
                  "merchantInfo": {
                    "merchantId": "a9c12627-a429-49c1-b580-a938c86fd9c7",
                    "merchantName": "GPay"
                  },
                  "transactionInfo": {
                    "totalPriceStatus": "FINAL",
                    "totalPriceLabel": "Total",
                    "totalPrice": "56.56",
                    "currencyCode": "AED"
                  }
                }
            """.trimIndent()
        val isReadyToPayRequestJson = """[
                    {
                      "type": "CARD",
                      "parameters": {
                        "allowedAuthMethods": [
                          "PAN_ONLY",
                          "CRYPTOGRAM_3DS"
                        ],
                        "allowedCardNetworks": [
                          "VISA",
                          "MASTERCARD"
                        ]
                      },
                      "tokenizationSpecification": {
                        "type": "PAYMENT_GATEWAY",
                        "parameters": {
                          "gateway": "networkintl",
                          "gatewayMerchantId": "a9c12627-a429-49c1-b580-a938c86fd9c7"
                        }
                      }
                    }
                  ]"""
    }
}