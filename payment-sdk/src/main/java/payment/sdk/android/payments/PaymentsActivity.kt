package payment.sdk.android.payments

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.view.View
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.core.os.LocaleListCompat
import androidx.core.text.TextUtilsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.contract.TaskResultContracts.GetPaymentDataResult
import kotlinx.coroutines.launch
import payment.sdk.android.payments.view.PaymentsScreen
import payment.sdk.android.cardpayment.CardPaymentData
import payment.sdk.android.cardpayment.partialAuth.model.PartialAuthActivityArgs
import payment.sdk.android.cardpayment.savedCard.SavedCardPaymentActivity.Companion.THREE_D_SECURE_REQUEST_KEY
import payment.sdk.android.cardpayment.savedCard.SavedCardPaymentActivity.Companion.THREE_D_SECURE_TWO_REQUEST_KEY
import payment.sdk.android.cardpayment.savedCard.SavedCardPaymentActivity.Companion.VISA_INSTALMENT_SELECTION_KEY
import payment.sdk.android.cardpayment.threedsecure.ThreeDSecureWebViewActivity
import payment.sdk.android.cardpayment.threedsecuretwo.webview.PartialAuthIntent
import payment.sdk.android.cardpayment.threedsecuretwo.webview.ThreeDSecureTwoWebViewActivity
import payment.sdk.android.cardpayment.threedsecuretwo.webview.ThreeDSecureTwoWebViewActivity.Companion.INTENT_CHALLENGE_RESPONSE
import payment.sdk.android.cardpayment.visaInstalments.model.VisaInstalmentActivityArgs
import payment.sdk.android.cardpayment.widget.CircularProgressDialog
import payment.sdk.android.core.CardType
import payment.sdk.android.core.OrderAmount
import payment.sdk.android.sdk.R
import java.util.Locale

class PaymentsActivity : AppCompatActivity() {

    private val viewModel: PaymentsViewModel by viewModels { PaymentsViewModel.Factory(args) }

    private lateinit var args: CardPaymentsLauncher.CardPaymentsIntent

    private val paymentDataLauncher =
        registerForActivityResult(GetPaymentDataResult()) { taskResult ->
            when (taskResult.status.statusCode) {
                CommonStatusCodes.SUCCESS -> {
                    taskResult.result?.let {
                        viewModel.acceptGooglePay(it.toJson())
                    }
                }

                CommonStatusCodes.CANCELED -> finishWithData(CardPaymentsLauncher.Result.Cancelled)

                AutoResolveHelper.RESULT_ERROR ->
                    finishWithData(CardPaymentsLauncher.Result.Failed("Google Pay error"))

                CommonStatusCodes.INTERNAL_ERROR ->
                    finishWithData(CardPaymentsLauncher.Result.Failed("Google Pay error"))
            }
        }

    private val partialAuthActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            finishWithData(CardPaymentData.getCardPaymentState(result.data))
        } else {
            finishWithData(CardPaymentsLauncher.Result.Failed("Partial auth failed"))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        args = runCatching {
            requireNotNull(CardPaymentsLauncher.CardPaymentsIntent.fromIntent(intent)) {
                "GooglePayActivity input arguments were not found"
            }
        }.getOrElse {
            finishWithData(CardPaymentsLauncher.Result.Failed("intent args not found"))
            return
        }
        initEffects()
        setContent {
            Scaffold(
                backgroundColor = Color(0xFFD6D6D6),
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(id = R.string.make_payment),
                                color = colorResource(id = R.color.payment_sdk_pay_button_text_color)
                            )
                        },
                        backgroundColor = colorResource(id = R.color.payment_sdk_toolbar_color),
                        navigationIcon = {
                            IconButton(onClick = {
                                finishWithData(CardPaymentsLauncher.Result.Cancelled)
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    tint = colorResource(id = R.color.payment_sdk_toolbar_icon_color),
                                    contentDescription = "Back"
                                )
                            }
                        }
                    )
                },
            ) { contentPadding ->
                val state by viewModel.uiState.collectAsState()
                when (state) {
                    is PaymentsVMUiState.Authorized -> {
                        val authState = (state as PaymentsVMUiState.Authorized)
                        PaymentsScreen(
                            modifier = Modifier.padding(contentPadding),
                            supportedCards = authState.supportedCards.toMutableSet().apply {
                                add(CardType.Visa)
                            },
                            googlePayConfig = authState.googlePayConfig,
                            onMakePayment = { cardNumber, expiry, cvv, cardholderName ->
                                viewModel.makeCardPayment(
                                    selfUrl = authState.selfUrl,
                                    cardPaymentUrl = authState.cardPaymentUrl,
                                    accessToken = authState.accessToken,
                                    paymentCookie = authState.paymentCookie,
                                    cardNumber = cardNumber,
                                    expiry = expiry,
                                    cvv = cvv,
                                    cardholderName = cardholderName,
                                    orderUrl = authState.orderUrl,
                                    amount = authState.amount,
                                    currencyCode = authState.currencyCode
                                )
                            },
                            formattedAmount = authState.orderAmount,
                            showWallets = authState.showWallets,
                            onGooglePay = {
                                authState.googlePayConfig?.task?.addOnCompleteListener(
                                    paymentDataLauncher::launch
                                )
                            }
                        )
                    }

                    PaymentsVMUiState.Init -> {
                        viewModel.authorize()
                    }

                    is PaymentsVMUiState.Loading -> {
                        CircularProgressDialog((state as PaymentsVMUiState.Loading).message)
                    }
                }
            }
        }
    }

    private fun localeSelection(context: Context, localeTag: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java).applicationLocales =
                LocaleList.forLanguageTags(localeTag)
        } else {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(localeTag)
            )
        }
    }

    private fun initEffects() {
        lifecycleScope.launch {
            viewModel.effect.collect {
                when (it) {
                    PaymentsVMEffects.Captured -> finishWithData(CardPaymentsLauncher.Result.Success)
                    is PaymentsVMEffects.Failed -> finishWithData(
                        CardPaymentsLauncher.Result.Failed(
                            it.error
                        )
                    )

                    is PaymentsVMEffects.InitiatePartialAuth -> {
                        try {
                            partialAuthActivityLauncher.launch(
                                PartialAuthActivityArgs.getArgs(
                                    it.partialAuthIntent
                                ).toIntent(this@PaymentsActivity)
                            )
                        } catch (e: IllegalArgumentException) {
                            finishWithData(CardPaymentsLauncher.Result.Failed(""))
                        }
                    }

                    is PaymentsVMEffects.InitiateThreeDS -> {
                        val response = it.threeDSecureDto
                        startActivityForResult(
                            ThreeDSecureWebViewActivity.getIntent(
                                context = this@PaymentsActivity,
                                acsUrl = response.acsUrl,
                                acsPaReq = response.acsPaReq,
                                acsMd = response.acsMd,
                                gatewayUrl = response.threeDSOneUrl
                            ),
                            THREE_D_SECURE_REQUEST_KEY
                        )
                    }

                    is PaymentsVMEffects.InitiateThreeDSTwo -> {
                        val response = it.threeDSecureTwoDto
                        startActivityForResult(
                            ThreeDSecureTwoWebViewActivity.getIntent(
                                context = this@PaymentsActivity,
                                threeDSMethodData = response.threeDSMethodData,
                                threeDSMethodNotificationURL = response.threeDSMethodNotificationURL,
                                threeDSMethodURL = response.threeDSMethodURL,
                                threeDSServerTransID = response.threeDSServerTransID,
                                paymentCookie = response.paymentCookie,
                                threeDSAuthenticationsUrl = response.threeDSTwoAuthenticationURL,
                                directoryServerID = response.directoryServerID,
                                threeDSMessageVersion = response.threeDSMessageVersion,
                                threeDSTwoChallengeResponseURL = response.threeDSTwoChallengeResponseURL,
                                outletRef = response.outletRef,
                                orderRef = response.orderRef,
                                orderUrl = response.orderUrl,
                                paymentRef = response.paymentReference
                            ),
                            THREE_D_SECURE_TWO_REQUEST_KEY
                        )
                    }

                    PaymentsVMEffects.PaymentAuthorised -> finishWithData(CardPaymentsLauncher.Result.PartiallyAuthorised)
                    PaymentsVMEffects.PostAuthReview -> finishWithData(CardPaymentsLauncher.Result.PostAuthReview)
                    PaymentsVMEffects.Purchased -> finishWithData(CardPaymentsLauncher.Result.Success)
                    is PaymentsVMEffects.ShowVisaPlans -> {
                        startActivityForResult(
                            VisaInstalmentActivityArgs.getArgs(
                                paymentCookie = it.paymentCookie,
                                savedCardUrl = null,
                                visaPlans = it.visaPlans,
                                paymentUrl = it.cardPaymentUrl,
                                newCard = it.newCardDto,
                                payPageUrl = args.paymentUrl,
                                savedCard = null,
                                orderUrl = it.orderUrl,
                                orderAmount = OrderAmount(it.amount, it.currencyCode),
                                accessToken = it.paymentCookie
                            ).toIntent(this@PaymentsActivity),
                            VISA_INSTALMENT_SELECTION_KEY
                        )
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_CANCELED) {
            return finishWithData(CardPaymentsLauncher.Result.Cancelled)
        }

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                THREE_D_SECURE_REQUEST_KEY, THREE_D_SECURE_TWO_REQUEST_KEY -> {
                    runCatching {
                        val state =
                            requireNotNull(data?.getStringExtra(ThreeDSecureWebViewActivity.KEY_3DS_STATE)) {
                                "State is missing from 3DS Secure result"
                            }
                        when (state) {
                            "AUTHORISED" -> finishWithData(CardPaymentsLauncher.Result.Authorised)
                            "PURCHASED", "CAPTURED" -> finishWithData(CardPaymentsLauncher.Result.Success)
                            "FAILED" -> finishWithData(CardPaymentsLauncher.Result.Failed("3DS Failed"))
                            "POST_AUTH_REVIEW" -> finishWithData(CardPaymentsLauncher.Result.PostAuthReview)
                            "AWAITING_PARTIAL_AUTH_APPROVAL" -> {
                                runCatching {
                                    requireNotNull(
                                        data?.getParcelableExtra(
                                            INTENT_CHALLENGE_RESPONSE
                                        ) as? PartialAuthIntent
                                    ) {
                                        "Partial auth intent is missing"
                                    }
                                }.getOrElse {
                                    finishWithData(CardPaymentsLauncher.Result.Failed(it.message.orEmpty()))
                                    return
                                }.let {
                                    startPartialAuthActivity(it)
                                }
                            }

                            else -> finishWithData(CardPaymentsLauncher.Result.Failed("3DS Failed"))
                        }
                    }.onFailure {
                        finishWithData(
                            CardPaymentsLauncher.Result.Failed(
                                it.message ?: "Failed 3DS"
                            )
                        )
                    }
                }

                VISA_INSTALMENT_SELECTION_KEY -> {
                    finishWithData(CardPaymentsLauncher.Result.Success)
                }
            }
        } else {
            return finishWithData(CardPaymentsLauncher.Result.Failed("Failed 3DS"))
        }
    }

    private fun startPartialAuthActivity(partialAuthIntent: PartialAuthIntent) {
        try {
            partialAuthActivityLauncher.launch(
                PartialAuthActivityArgs.getArgs(partialAuthIntent).toIntent(this)
            )
        } catch (e: IllegalArgumentException) {
            finishWithData(CardPaymentsLauncher.Result.Failed(e.message.orEmpty()))
        }
    }

    private fun finishWithData(result: CardPaymentsLauncher.Result) {
        val intent = Intent().apply {
            putExtra(PaymentsLauncherContract.EXTRA_RESULT, result)
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }
}