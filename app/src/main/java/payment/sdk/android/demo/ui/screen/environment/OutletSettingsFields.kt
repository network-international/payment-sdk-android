package payment.sdk.android.demo.ui.screen.environment

import androidx.compose.runtime.Composable
import payment.sdk.android.demo.model.AppCurrency
import payment.sdk.android.demo.model.OrderAction
import payment.sdk.android.demo.model.OrderType
import payment.sdk.android.demo.model.Region

/**
 * Region, currency, order action and order type for one outlet.
 *
 * These four are outlet configuration, not app settings: a key only works against its own
 * region's deployment, alternative payment methods follow the acquirer (so the currency
 * decides what is offered — a BHD outlet is the Benefit one, a QAR outlet the QPay one),
 * and which actions and order types an outlet accepts differ per outlet. They used to sit
 * in the main configuration screen, where one global value was necessarily wrong for every
 * outlet but the one it happened to be set for.
 */
@Composable
fun OutletSettingsFields(
    region: Region,
    currency: AppCurrency,
    orderAction: OrderAction,
    orderType: OrderType,
    idPrefix: String,
    onRegionChange: (Region) -> Unit,
    onCurrencyChange: (AppCurrency) -> Unit,
    onOrderActionChange: (OrderAction) -> Unit,
    onOrderTypeChange: (OrderType) -> Unit,
) {
    PickerView(
        title = "Region",
        items = Region.entries,
        selectedItem = region,
        pickerId = "${idPrefix}_picker_region",
    ) { onRegionChange(it as Region) }

    PickerView(
        title = "Currency",
        items = AppCurrency.entries,
        selectedItem = currency,
        pickerId = "${idPrefix}_picker_currency",
    ) { onCurrencyChange(it as AppCurrency) }

    PickerView(
        title = "Order Action",
        items = OrderAction.entries,
        selectedItem = orderAction,
        pickerId = "${idPrefix}_picker_orderAction",
    ) { onOrderActionChange(it as OrderAction) }

    PickerView(
        title = "Order Type",
        items = OrderType.entries,
        selectedItem = orderType,
        pickerId = "${idPrefix}_picker_orderType",
    ) { onOrderTypeChange(it as OrderType) }
}

/** Resolves a stored code to its enum, falling back rather than throwing on an unknown value. */
fun regionOf(code: String?): Region =
    Region.entries.firstOrNull { it.code == code } ?: Region.UAE

fun currencyOf(code: String?): AppCurrency =
    AppCurrency.entries.firstOrNull { it.code == code } ?: AppCurrency.AED

fun orderActionOf(code: String?): OrderAction =
    OrderAction.entries.firstOrNull { it.code == code } ?: OrderAction.SALE

fun orderTypeOf(code: String?): OrderType =
    OrderType.entries.firstOrNull { it.code == code } ?: OrderType.SINGLE
