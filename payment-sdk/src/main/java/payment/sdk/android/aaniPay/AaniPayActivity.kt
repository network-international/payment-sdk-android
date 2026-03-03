package payment.sdk.android.aaniPay

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import payment.sdk.android.aaniPay.views.AaniPayScreen
import payment.sdk.android.aaniPay.views.AaniPayTimerScreen
import payment.sdk.android.aaniPay.views.AaniQrDisplayScreen
import payment.sdk.android.aaniPay.views.AaniQrLoadingScreen
import payment.sdk.android.aaniPay.views.AaniQrStatusScreen
import payment.sdk.android.aaniPay.views.QrStatusType
import payment.sdk.android.cardpayment.theme.sdkColor
import payment.sdk.android.aaniPay.model.AaniPayVMState
import payment.sdk.android.cardpayment.widget.CircularProgressDialog
import payment.sdk.android.sdk.R

class AaniPayActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val lang = payment.sdk.android.SDKConfig.getLanguage()
        val locale = java.util.Locale(lang)
        val config = newBase.resources.configuration.apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    private val inputArgs: AaniPayLauncher.Config? by lazy {
        AaniPayLauncher.Config.fromIntent(intent = intent)
    }

    private val viewModel: AaniPayViewModel by viewModels { AaniPayViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = runCatching {
            requireNotNull(inputArgs) {
                "AaniPayActivity input arguments were not found"
            }
        }.getOrElse {
            finishWithData(AaniPayLauncher.Result.Failed(it.message.orEmpty()))
            return
        }

        lifecycleScope.launch {
            viewModel.state.collect { state ->
                handleBackPress(state)
                if (state == AaniPayVMState.Cancelled) {
                    finishWithData(AaniPayLauncher.Result.Canceled)
                }
            }
        }

        setContent {
            val state by viewModel.state.collectAsState()

            Scaffold(topBar = {
                TopAppBar(title = {
                    Text(
                        text = stringResource(R.string.aani),
                        color = sdkColor(R.color.payment_sdk_pay_button_text_color)
                    )
                },
                    backgroundColor = sdkColor(R.color.payment_sdk_toolbar_color),
                    navigationIcon = {
                        if (state !is AaniPayVMState.Pooling && state !is AaniPayVMState.QrDisplay && state !is AaniPayVMState.Loading && state !is AaniPayVMState.QrLoading) {
                            IconButton(onClick = { finishWithData(AaniPayLauncher.Result.Canceled) }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    tint = sdkColor(R.color.payment_sdk_toolbar_icon_color),
                                    contentDescription = "Back"
                                )
                            }
                        }
                    })
            }) { contentPadding ->
                Column(
                    modifier = Modifier.padding(contentPadding)
                ) {
                    when (state) {
                        is AaniPayVMState.Init -> {
                            AaniPayScreen(
                                qrEnabled = args.anniQrPaymentLink.isNotEmpty()
                            ) { alias, value ->
                                viewModel.onSubmit(
                                    args = args,
                                    alias = alias,
                                    value = value,
                                    accessToken = args.accessToken,
                                    payerIp = args.payerIp
                                )
                            }
                        }

                        is AaniPayVMState.Error -> {
                            finishWithData(AaniPayLauncher.Result.Failed((state as AaniPayVMState.Error).message))
                        }

                        is AaniPayVMState.Pooling -> {
                            AaniPayTimerScreen(
                                (state as AaniPayVMState.Pooling).amount,
                                (state as AaniPayVMState.Pooling).currencyCode
                            )
                            try {
                                startActivity(Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse((state as AaniPayVMState.Pooling).deepLink)
                                ).apply {
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        is AaniPayVMState.QrDisplay -> {
                            AaniQrDisplayScreen(
                                (state as AaniPayVMState.QrDisplay).amount,
                                (state as AaniPayVMState.QrDisplay).currencyCode,
                                (state as AaniPayVMState.QrDisplay).qrContent,
                                onExpired = { viewModel.onQrExpired() },
                                onCancel = { viewModel.cancelQr() })
                        }

                        is AaniPayVMState.QrExpired -> {
                            AaniQrStatusScreen(
                                statusType = QrStatusType.EXPIRED,
                                onAction = { viewModel.retryQr() },
                                onCancel = { viewModel.cancelQr() }
                            )
                        }

                        is AaniPayVMState.QrFailed -> {
                            AaniQrStatusScreen(
                                statusType = QrStatusType.FAILED,
                                onAction = { viewModel.retryQr() },
                                onCancel = { viewModel.cancelQr() }
                            )
                        }

                        is AaniPayVMState.PaymentTimeout -> {
                            AaniQrStatusScreen(
                                statusType = QrStatusType.TIMEOUT,
                                onAction = { viewModel.retryPayment() },
                                onCancel = { viewModel.cancelQr() }
                            )
                        }

                        AaniPayVMState.Cancelled -> {
                            finishWithData(AaniPayLauncher.Result.Canceled)
                        }

                        is AaniPayVMState.Loading -> {
                            CircularProgressDialog((state as AaniPayVMState.Loading).message)
                        }

                        AaniPayVMState.QrLoading -> {
                            AaniQrLoadingScreen()
                        }

                        AaniPayVMState.Success -> {
                            finishWithData(AaniPayLauncher.Result.Success)
                        }
                    }
                }
            }
        }
    }

    private fun handleBackPress(state: AaniPayVMState) {
        val isBlocked = state is AaniPayVMState.Pooling ||
                state is AaniPayVMState.QrDisplay ||
                state is AaniPayVMState.Loading ||
                state is AaniPayVMState.QrLoading
        onBackPressedDispatcher.addCallback(this) {
            if (!isBlocked) {
                finish()
            }
        }.apply {
            isEnabled = isBlocked
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun finishWithData(result: AaniPayLauncher.Result) {
        val intent = Intent().apply {
            putExtra(AaniPayLauncherContract.EXTRA_RESULT, result)
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }
}