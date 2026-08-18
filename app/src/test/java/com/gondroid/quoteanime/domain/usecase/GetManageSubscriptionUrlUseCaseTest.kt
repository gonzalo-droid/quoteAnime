package com.gondroid.quoteanime.domain.usecase

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The deep link is the app's only cancellation path — a malformed one silently drops the user
 * on Play's generic subscription list instead of this subscription.
 */
class GetManageSubscriptionUrlUseCaseTest {

    @Test
    fun `builds the Play subscription centre deep link for this product and package`() {
        val context = mockk<Context>()
        every { context.packageName } returns "com.gondroid.quoteanime"

        assertEquals(
            "https://play.google.com/store/account/subscriptions" +
                    "?sku=premium_subscription&package=com.gondroid.quoteanime",
            GetManageSubscriptionUrlUseCase(context)()
        )
    }
}
