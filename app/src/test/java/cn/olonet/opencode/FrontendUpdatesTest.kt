package cn.olonet.opencode

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FrontendUpdatesTest {
    private val release = FrontendRelease(2, "0.1.2", "http://example.com/dist.zip", "new-sha", "")

    @Test fun firstInstallRequiresDownload() {
        assertTrue(FrontendUpdates.needsUpdate(0, null, release))
    }
    @Test fun sameHashNeverRedownloadsEvenWhenVersionIncreases() {
        assertFalse(FrontendUpdates.needsUpdate(1, "new-sha", release))
    }
    @Test fun sameOrOlderVersionDoesNotDownloadChangedContent() {
        assertFalse(FrontendUpdates.needsUpdate(2, "old-sha", release))
        assertFalse(FrontendUpdates.needsUpdate(3, "old-sha", release))
    }
    @Test fun newerVersionWithDifferentHashNeedsConfirmationAndDownload() {
        assertTrue(FrontendUpdates.needsUpdate(1, "old-sha", release))
    }
}
