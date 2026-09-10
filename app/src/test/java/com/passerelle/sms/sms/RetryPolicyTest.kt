package com.passerelle.sms.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RetryPolicyTest {
    @Test
    fun retriesWhileAttemptsRemain() {
        assertTrue(RetryPolicy.shouldRetry(1, 3))
        assertTrue(RetryPolicy.shouldRetry(2, 3))
        assertFalse(RetryPolicy.shouldRetry(3, 3))
    }

    @Test
    fun noRetryWhenMaxIsOne() {
        assertFalse(RetryPolicy.shouldRetry(1, 1))
    }

    @Test
    fun nextAttemptWaitsTheDelay() {
        assertEquals(5_000L, RetryPolicy.nextAttemptAt(1_000L, 4_000))
    }
}
