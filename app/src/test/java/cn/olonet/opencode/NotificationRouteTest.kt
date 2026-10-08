package cn.olonet.opencode

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationRouteTest {
    @Test fun acceptsOnlyLocalFrontendRoutes() {
        val route = "http://localhost:8080/server/session/ses_123?tab=messages#latest"
        assertEquals(route, NotificationRoute.validate(route))
    }

    @Test fun rejectsExternalAndMalformedDestinations() {
        listOf(
            "https://example.com/", "javascript:alert(1)", "file:///etc/passwd",
            "http://localhost:8081/", "https://localhost:8080/", "http://localhost/",
            "http://user@localhost:8080/", "http://localhost:8080.evil.com/",
            "//localhost:8080/", "/session/123", "http://localhost:8080/\\evil",
            "http://localhost:8080/\n", "http://localhost:8080/%zz",
            "http://localhost:8080/" + "x".repeat(4096),
        ).forEach { assertNull(it, NotificationRoute.validate(it)) }
    }
}
