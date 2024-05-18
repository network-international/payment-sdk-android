package payment.sdk.android.demo.model

import org.junit.Assert.assertEquals
import org.junit.Test

class EnvironmentTest {

    @Test
    fun `getGatewayUrl and getIdentityUrl return correct URLs for all environment types`() {
        val environments = listOf(
            Environment(EnvironmentType.DEV, name = "Dev Environment", apiKey = "api_key", outletReference = "outlet_ref", realm = "realm"),
            Environment(EnvironmentType.UAT, name = "UAT Environment", apiKey = "api_key", outletReference = "outlet_ref", realm = "realm"),
            Environment(EnvironmentType.PROD, name = "Prod Environment", apiKey = "api_key", outletReference = "outlet_ref", realm = "realm")
        )

        environments.forEach { environment ->
            val gatewayUrl = when (environment.type) {
                EnvironmentType.DEV -> "https://api-gateway-dev.ngenius-payments.com/transactions/outlets/outlet_ref/orders"
                EnvironmentType.UAT -> "https://api-gateway-uat.ngenius-payments.com/transactions/outlets/outlet_ref/orders"
                EnvironmentType.PROD -> "https://api-gateway.ngenius-payments.com/transactions/outlets/outlet_ref/orders"
            }

            val identityUrl = when (environment.type) {
                EnvironmentType.DEV -> "https://api-gateway-dev.ngenius-payments.com/identity/auth/access-token"
                EnvironmentType.UAT -> "https://api-gateway-uat.ngenius-payments.com/identity/auth/access-token"
                EnvironmentType.PROD -> "https://api-gateway.ngenius-payments.com/identity/auth/access-token"
            }

            assertEquals(gatewayUrl, environment.getGatewayUrl())
            assertEquals(identityUrl, environment.getIdentityUrl())
        }
    }
}
