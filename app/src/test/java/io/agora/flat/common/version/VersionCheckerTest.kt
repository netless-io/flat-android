package io.agora.flat.common.version

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionCheckerTest {

    @Test
    fun `checkCanUpdate$app_debug`() {
        assertTrue(VersionChecker.checkCanUpdate("1.5.2", "1.5.3"))
        assertTrue(VersionChecker.checkCanUpdate("1.5.2", "2.0.1"))
        assertFalse(VersionChecker.checkCanUpdate("1.5.2", "1.4.6"))
        assertFalse(VersionChecker.checkCanUpdate("1.5.2", "1.5.2"))
        assertFalse(VersionChecker.checkCanUpdate("1.5.3.112", "1.5.2"))
    }

    @Test
    fun `checkForceUpdate$app_debug`() {
        assertTrue(VersionChecker.checkForceUpdate("1.3.6", "1.4.0"))
        assertTrue(VersionChecker.checkForceUpdate("1.3.6", "2.0.0"))
        assertFalse(VersionChecker.checkCanUpdate("1.3.6", "1.2.9"))
        assertFalse(VersionChecker.checkForceUpdate("1.3.6", "1.3.6"))
        assertFalse(VersionChecker.checkForceUpdate("1.5.3.112", "1.5.2"))
        assertFalse(VersionChecker.checkForceUpdate("1.4.0", "1.4.0"))
    }
}