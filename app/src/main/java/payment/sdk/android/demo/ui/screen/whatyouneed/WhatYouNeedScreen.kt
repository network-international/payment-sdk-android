package payment.sdk.android.demo.ui.screen.whatyouneed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatYouNeedScreen(onNavUp: () -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "What You Need") },
                navigationIcon = {
                    IconButton(onClick = onNavUp, modifier = Modifier.testTag("whatyouneed_button_back")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        content = { contentPadding ->
            Column(
                modifier = Modifier
                    .padding(contentPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // N-Genius Portal (Required)
                CategoryHeader(title = "N-Genius Portal (Required)")

                SetupItemCard(
                    name = "API Key",
                    description = "Base64-encoded authentication credential",
                    source = "N-Genius Portal \u2192 Settings \u2192 API Keys",
                    codeLocation = "Settings \u2192 Environments \u2192 Add Environment"
                )
                SetupItemCard(
                    name = "Outlet Reference",
                    description = "Unique identifier for your merchant outlet",
                    source = "N-Genius Portal \u2192 Settings \u2192 Organizational Hierarchy \u2192 Outlet",
                    codeLocation = "Settings \u2192 Environments \u2192 Add Environment"
                )
                SetupItemCard(
                    name = "Realm",
                    description = "Authentication realm for your organization",
                    source = "N-Genius Portal \u2192 Settings \u2192 Organizational Hierarchy",
                    codeLocation = "Settings \u2192 Environments \u2192 Add Environment"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Google Pay (Optional)
                CategoryHeader(title = "Google Pay (Optional)")

                SetupItemCard(
                    name = "Merchant Gateway ID",
                    description = "Google Pay merchant gateway identifier",
                    source = "Google Pay Business Console (pay.google.com/business/console)",
                    codeLocation = "MainActivity.kt \u2192 launchPaymentPage() \u2192 GooglePayConfig"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Samsung Pay (Optional)
                CategoryHeader(title = "Samsung Pay (Optional)")

                SetupItemCard(
                    name = "Service ID",
                    description = "Samsung Pay service identifier",
                    source = "Samsung Pay Developer Portal (pay.samsung.com/developers)",
                    codeLocation = "MainActivity.kt \u2192 PaymentClient(this, \"serviceId\")"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Click to Pay (Optional)
                CategoryHeader(title = "Click to Pay (Optional)")

                SetupItemCard(
                    name = "DPA ID",
                    description = "Digital Payment Application identifier",
                    source = "Network International / Click to Pay onboarding",
                    codeLocation = "MainActivity.kt \u2192 launchPaymentPage() \u2192 ClickToPayConfig"
                )
                SetupItemCard(
                    name = "DPA Client ID",
                    description = "Digital Payment Application client identifier",
                    source = "Network International / Click to Pay onboarding",
                    codeLocation = "MainActivity.kt \u2192 launchPaymentPage() \u2192 ClickToPayConfig"
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    )
}

@Composable
private fun CategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(vertical = 8.dp)
    )
    HorizontalDivider()
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun SetupItemCard(
    name: String,
    description: String,
    source: String,
    codeLocation: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = "Where to get it:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = source,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = "Where to set it:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = codeLocation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
