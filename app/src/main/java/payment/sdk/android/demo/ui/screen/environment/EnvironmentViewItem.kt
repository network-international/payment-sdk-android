package payment.sdk.android.demo.ui.screen.environment

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import payment.sdk.android.sdk.R
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Check
import payment.sdk.android.demo.model.Environment
import payment.sdk.android.core.testId

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EnvironmentViewItem(
    environment: Environment,
    isSelected: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        shadowElevation = 0.dp,
        // Every cell gets a border, not just the selected one. With only an
        // elevation shadow the unselected rows had no visible edge on a light
        // background, so it was unclear where one outlet ended and the next began.
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp, horizontal = 2.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable {
                if (!isSelected) {
                    onClick()
                }
            }
            .testId("environment_item_${environment.name}")
    ) {
        // A Column, not a Row: the actions used to sit beside the text and reserve a
        // fixed gutter, so anything below wrapped after two or three entries. Here the
        // actions have their own row and everything under them spans the card.
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = environment.nickname.orEmpty().ifEmpty { environment.realm },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        text = environment.type.value,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                    )
                }
                IconButton(
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(28.dp)
                        .testId("environment_button_edit_${environment.name}"),
                    onClick = onEdit,
                ) { Icon(Icons.Filled.Edit, "Edit environment") }
                IconButton(
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(28.dp)
                        .testId("environment_button_delete_${environment.name}"),
                    onClick = onDelete,
                ) { Icon(Icons.Filled.Delete, "Delete environment") }
            }

            Text(
                text = environment.realm,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = "${environment.region.code} · ${environment.currency ?: "AED"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )

            val methods = environment.paymentMethods.orEmpty()
            val named = namedPaymentMethods(methods)
            if (named.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    named.forEach { label ->
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }

            if (methods.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                val enabled = enabledCardBrands(methods)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CARD_BRANDS.forEach { brand ->
                        CardBrandChip(brand, enabled.contains(brand.name))
                    }
                }
            }
        }
    }
}

/** A card scheme the tile can draw: the gateway's name and the SDK's logo. */
private data class CardBrand(val name: String, val drawable: Int, val label: String)

private val CARD_BRANDS = listOf(
    CardBrand("MASTERCARD", R.drawable.ic_logo_mastercard, "Mastercard"),
    CardBrand("VISA", R.drawable.ic_logo_visa, "Visa"),
    CardBrand("AMERICAN_EXPRESS", R.drawable.ic_logo_amex, "Amex"),
    CardBrand("DINERS_CLUB_INTERNATIONAL", R.drawable.ic_logo_dinners_club, "Diners"),
    CardBrand("DISCOVER", R.drawable.ic_logo_discover, "Discover"),
    CardBrand("JCB", R.drawable.ic_logo_jcb, "JCB"),
)

/**
 * Which schemes this outlet accepts. Matched loosely because the gateway's names vary
 * ("AMERICAN_EXPRESS" / "AMEX"), the same way the SDK's own brand strip matches them.
 */
private fun enabledCardBrands(methods: List<String>): Set<String> {
    val up = methods.map { it.uppercase() }
    fun has(vararg needles: String) = up.any { m -> needles.any { m.contains(it) } }
    return buildSet {
        if (has("MASTER")) add("MASTERCARD")
        // "VISA_CLICK_TO_PAY" is a wallet, not the card scheme.
        if (up.any { it == "VISA" }) add("VISA")
        if (has("AMERICAN", "AMEX")) add("AMERICAN_EXPRESS")
        if (has("DINERS")) add("DINERS_CLUB_INTERNATIONAL")
        if (has("DISCOVER")) add("DISCOVER")
        if (has("JCB")) add("JCB")
    }
}

/**
 * The named payment methods an outlet offers, as the demo talks about them.
 *
 * The gateway reports a hosted wallet and the merchant's own in-app integration as
 * separate names (APPLE_PAY / DIRECT_APPLE_PAY); both mean "this outlet does Apple Pay",
 * so they collapse to one entry. Card schemes are excluded — they are shown as logos.
 */
internal fun namedPaymentMethods(raw: List<String>): List<String> {
    val labels = mapOf(
        "APPLE_PAY" to "Apple Pay",
        "GOOGLE_PAY" to "Google Pay",
        "SAMSUNG_PAY" to "Samsung Pay",
        "VISA_CLICK_TO_PAY" to "Click to Pay",
        "AANI" to "Aani",
        "QPAY" to "QPay",
        "SLICE" to "Slice",
        "TAMARA" to "Tamara",
        "TABBY" to "Tabby",
        "BENEFIT" to "Benefit",
        "NAPS" to "NAPS",
        "VISA_INSTALMENTS" to "Visa instalments",
    )
    val order = listOf(
        "Apple Pay", "Google Pay", "Samsung Pay", "Click to Pay", "Aani", "QPay",
        "Visa instalments", "Slice", "Tamara", "Tabby", "Benefit", "NAPS",
    )
    val found = raw.mapNotNull { labels[it.removePrefix("DIRECT_").uppercase()] }.toSet()
    return order.filter { it in found } + found.filterNot { it in order }
}

/** A scheme logo with its status on the bottom edge: accepted or not. */
@Composable
private fun CardBrandChip(brand: CardBrand, enabled: Boolean) {
    Box(modifier = Modifier.size(width = 40.dp, height = 30.dp)) {
        Image(
            painter = painterResource(id = brand.drawable),
            contentDescription = brand.label,
            contentScale = ContentScale.Fit,
            // Dimmed rather than hidden: the mark still has to be identifiable.
            alpha = if (enabled) 1f else 0.35f,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(width = 40.dp, height = 20.dp),
        )
        // CheckCircle is a filled disc; Close is a bare glyph, so it gets its own
        // disc to keep the two badges the same visual weight.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(13.dp)
                .background(
                    color = if (enabled) Color(0xFF2E7D32) else Color(0xFFE53935),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (enabled) Icons.Filled.Check else Icons.Filled.Close,
                contentDescription = if (enabled) "accepted" else "not accepted",
                tint = Color.White,
                modifier = Modifier.size(9.dp),
            )
        }
    }
}
