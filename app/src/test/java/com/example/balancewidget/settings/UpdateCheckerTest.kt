package com.example.balancewidget.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {
    @Test
    fun recognizesNewerSemanticVersions() {
        assertTrue(UpdateChecker.isNewer("1.2.0", "1.1.9"))
        assertTrue(UpdateChecker.isNewer("v1.10.0", "1.2.0"))
    }

    @Test
    fun rejectsEqualAndOlderVersions() {
        assertFalse(UpdateChecker.isNewer("1.2.0", "1.2.0"))
        assertFalse(UpdateChecker.isNewer("1.1.9", "1.2.0"))
    }
}
