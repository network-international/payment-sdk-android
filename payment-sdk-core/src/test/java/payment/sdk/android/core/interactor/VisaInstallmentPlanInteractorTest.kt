package payment.sdk.android.core.interactor

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import payment.sdk.android.core.api.HttpClient
import payment.sdk.android.core.api.SDKHttpResponse

@OptIn(ExperimentalCoroutinesApi::class)
class VisaInstallmentPlanInteractorTest {
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    private val httpClient: HttpClient = mockk(relaxed = true)

    private lateinit var sut: VisaInstallmentPlanInteractor

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sut = VisaInstallmentPlanInteractor(httpClient)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `return success response with correct data`() = runTest {
        coEvery {
            httpClient.post(any(), any(), any())
        } returns SDKHttpResponse.Success(
            headers = mapOf(),
            body = response.trimIndent()
        )

        val response = sut.getPlans(
            selfUrl = "",
            cardNumber = "",
            token = ""
        )

        Assert.assertTrue(response is VisaPlansResponse.Success)
    }

    @Test
    fun `return failed when http response fails`() = runTest {
        coEvery {
            httpClient.post(any(), any(), any())
        } returns SDKHttpResponse.Failed(Exception("Network Error"))

        val response = sut.getPlans(
            selfUrl = "",
            cardNumber = "",
            token = ""
        )

        Assert.assertTrue(response is VisaPlansResponse.Error)
    }

    companion object {
        const val response = """
            {
    "matchedPlans": [
        {
            "costInfo": {
                "currency": "AED",
                "feeInfo": [
                    {
                        "ratePercentage": 250,
                        "type": "CONSUMER",
                        "flatFee": 5000
                    },
                    {
                        "ratePercentage": 0,
                        "type": "MERCHANT_FUNDING",
                        "flatFee": 0
                    },
                    {
                        "ratePercentage": 10,
                        "type": "MERCHANT_SERVICE",
                        "flatFee": 0
                    }
                ],
                "lastInstallment": {
                    "amount": 74000,
                    "installmentFee": 3516,
                    "totalAmount": 77516,
                    "upfrontFee": 0
                },
                "totalFees": 10550,
                "totalPlanCost": 232550,
                "totalRecurringFees": 10550,
                "totalUpfrontFees": 0,
                "annualPercentageRate": 0
            },
            "installmentFrequency": "MONTHLY",
            "name": "VISTest3MonthAED",
            "numberOfInstallments": 3,
            "termsAndConditions": [
                {
                    "text": "This is a sample text to describe the terms and conditions that govern the Visa Installment services.",
                    "version": 2,
                    "languageCode": "eng",
                    "url": "https://www.visa.com"
                },
                {
                    "text": "هذا نص نموذجي لوصف الشروط والأحكام التي تحكم خدمات أقساط التأشيرة.",
                    "version": 2,
                    "languageCode": "ara",
                    "url": "https://www.visa.com"
                }
            ],
            "fundedBy": [
                "CONSUMER"
            ],
            "type": "ISSUER_DEFAULT",
            "vPlanID": "r",
            "vPlanIDRef": "00000000CA"
        },
        {
            "costInfo": {
                "currency": "AED",
                "feeInfo": [
                    {
                        "ratePercentage": 0,
                        "type": "CONSUMER",
                        "flatFee": 1000
                    },
                    {
                        "ratePercentage": 0,
                        "type": "MERCHANT_FUNDING",
                        "flatFee": 0
                    },
                    {
                        "ratePercentage": 10,
                        "type": "MERCHANT_SERVICE",
                        "flatFee": 0
                    }
                ],
                "lastInstallment": {
                    "amount": 37000,
                    "installmentFee": 165,
                    "totalAmount": 37165,
                    "upfrontFee": 0
                },
                "totalFees": 1000,
                "totalPlanCost": 223000,
                "totalRecurringFees": 1000,
                "totalUpfrontFees": 0,
                "annualPercentageRate": 0
            },
            "installmentFrequency": "MONTHLY",
            "name": "VISTest6MonthAED",
            "numberOfInstallments": 6,
            "termsAndConditions": [
                {
                    "text": "This is a sample text to describe the terms and conditions that govern the Visa Installment services.",
                    "version": 2,
                    "languageCode": "eng",
                    "url": "https://www.visa.com"
                },
                {
                    "text": "هذا نص نموذجي لوصف الشروط والأحكام التي تحكم خدمات أقساط التأشيرة.",
                    "version": 2,
                    "languageCode": "ara",
                    "url": "https://www.visa.com"
                }
            ],
            "fundedBy": [
                "CONSUMER"
            ],
            "type": "ISSUER_DEFAULT",
            "vPlanID": "a",
            "vPlanIDRef": "a"
        },
        {
            "costInfo": {
                "currency": "AED",
                "feeInfo": [
                    {
                        "ratePercentage": 500,
                        "type": "CONSUMER",
                        "flatFee": 0
                    },
                    {
                        "ratePercentage": 0,
                        "type": "MERCHANT_FUNDING",
                        "flatFee": 0
                    },
                    {
                        "ratePercentage": 10,
                        "type": "MERCHANT_SERVICE",
                        "flatFee": 0
                    }
                ],
                "lastInstallment": {
                    "amount": 18500,
                    "installmentFee": 925,
                    "totalAmount": 19425,
                    "upfrontFee": 0
                },
                "totalFees": 11100,
                "totalPlanCost": 233100,
                "totalRecurringFees": 11100,
                "totalUpfrontFees": 0,
                "annualPercentageRate": 0
            },
            "installmentFrequency": "MONTHLY",
            "name": "VISTest12MonthAED",
            "numberOfInstallments": 12,
            "termsAndConditions": [
                {
                    "text": "This is a sample text to describe the terms and conditions that govern the Visa Installment services.",
                    "version": 2,
                    "languageCode": "eng",
                    "url": "https://www.visa.com"
                },
                {
                    "text": "هذا نص نموذجي لوصف الشروط والأحكام التي تحكم خدمات أقساط التأشيرة.",
                    "version": 2,
                    "languageCode": "ara",
                    "url": "https://www.visa.com"
                }
            ],
            "fundedBy": [
                "CONSUMER"
            ],
            "type": "ISSUER_DEFAULT",
            "vPlanID": "f",
            "vPlanIDRef": "00000000CC"
        }
    ]
}
        """
    }
}