package com.passerelle.sms.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneNormalizerTest {
    @Test
    fun frenchMobileBecomesE164() {
        assertEquals("+33612345678", PhoneNormalizer.normalize("06 12 34 56 78"))
    }

    @Test
    fun plusPrefixIsKept() {
        assertEquals("+33612345678", PhoneNormalizer.normalize("+33 6 12 34 56 78"))
    }

    @Test
    fun internationalZeroZero() {
        assertEquals("+33612345678", PhoneNormalizer.normalize("0033612345678"))
    }

    @Test
    fun rejectsTooShort() {
        assertNull(PhoneNormalizer.normalize("123"))
    }

    @Test
    fun rejectsBlank() {
        assertNull(PhoneNormalizer.normalize("   "))
    }
}
