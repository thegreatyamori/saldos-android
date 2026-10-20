package com.thegreatyamori.saldos.data.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RefreshPolicyTest {

    @Test
    fun `data only refreshes on mobile data`() {
        assertTrue(RefreshPolicy.DATA_ONLY.allowsAutomaticRefresh(onWifi = false))
        assertFalse(RefreshPolicy.DATA_ONLY.allowsAutomaticRefresh(onWifi = true))
    }

    @Test
    fun `always refreshes on any network`() {
        assertTrue(RefreshPolicy.ALWAYS.allowsAutomaticRefresh(onWifi = false))
        assertTrue(RefreshPolicy.ALWAYS.allowsAutomaticRefresh(onWifi = true))
    }

    @Test
    fun `manual never refreshes automatically`() {
        assertFalse(RefreshPolicy.MANUAL.allowsAutomaticRefresh(onWifi = false))
        assertFalse(RefreshPolicy.MANUAL.allowsAutomaticRefresh(onWifi = true))
    }
}
