package io.agora.flat.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlatExtensionsKtTest {

    @Test
    fun isValidPhone() {
        assertTrue("13022233344".isValidPhone())

        assertFalse("1302223334".isValidPhone())
        assertFalse("12022233344".isValidPhone())
        assertFalse("130222333440".isValidPhone())
    }

    @Test
    fun isValidSmsCode() {
        assertFalse("00".isValidSmsCode())
        assertTrue("0000".isValidSmsCode())
        assertTrue("000000".isValidSmsCode())
    }
}