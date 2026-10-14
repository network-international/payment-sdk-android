package payment.sdk.android.demo.ui.screen.environment

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import payment.sdk.android.core.testId
import payment.sdk.android.demo.model.AppCurrency
import payment.sdk.android.demo.model.OrderAction
import payment.sdk.android.demo.model.OrderType
import payment.sdk.android.demo.model.Region

/** Deep orange: "attention", not error. Same value as the Flutter demo's box. */
private val OverrideOn = Color(0xFFE65100)

/**
 * QA shortcut: region, currency, order action and order type for every order, in place of the
 * selected environment's own. Built to be obvious at a glance whether it is on — an override left
 * on by accident silently changes every order — and collapsed while off.
 */
@Composable
fun EnvironmentOverrideSection(
    state: OverrideState,
    onEnabledChange: (Boolean) -> Unit,
    onRegionChange: (Region) -> Unit,
    onCurrencyChange: (AppCurrency) -> Unit,
    onOrderActionChange: (OrderAction) -> Unit,
    onOrderTypeChange: (OrderType) -> Unit,
) {
    val on = state.enabled
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(shape)
            .background(if (on) OverrideOn.copy(alpha = 0.08f) else Color.Transparent)
            .border(if (on) 2.dp else 1.dp, if (on) OverrideOn else Color.LightGray, shape)
            .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 12.dp)
            .testId("environment_box_override"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                modifier = Modifier.testId("environment_checkbox_override"),
                checked = on,
                onCheckedChange = onEnabledChange,
                colors = CheckboxDefaults.colors(checkedColor = OverrideOn),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Override environment settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (on) {
                        "Every order uses the values below instead of the selected environment's."
                    } else {
                        "Off — each environment's own region, currency, action and type are used."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Surface(
                modifier = Modifier.padding(start = 8.dp),
                shape = RoundedCornerShape(10.dp),
                color = if (on) OverrideOn else Color(0xFFE0E0E0),
            ) {
                Text(
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                        .testId("environment_label_overrideState"),
                    text = if (on) "ON" else "OFF",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (on) Color.White else Color(0xFF616161),
                )
            }
        }

        // Collapsed while off: the values only matter when they apply.
        AnimatedVisibility(visible = on) {
            Column(modifier = Modifier.padding(start = 12.dp)) {
                OutletSettingsFields(
                    region = state.region,
                    currency = state.currency,
                    orderAction = orderActionOf(state.orderAction),
                    orderType = orderTypeOf(state.orderType),
                    idPrefix = "environment_override",
                    onRegionChange = onRegionChange,
                    onCurrencyChange = onCurrencyChange,
                    onOrderActionChange = onOrderActionChange,
                    onOrderTypeChange = onOrderTypeChange,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Region does not move the gateway or outlet; those come from the selected environment.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}
