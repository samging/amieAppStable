package org.example.project.backend

import kotlin.test.Test
import kotlin.test.assertEquals

class BackendConfigTest {
    private val config = BackendConfig(baseUrl = "http://backend.test/", siteUrl = "http://site.test/")

    @Test
    fun `urls are built without double slashes and absolute slugs pass through`() {
        assertEquals("http://backend.test/me", config.url("me"))
        assertEquals("http://backend.test/state/labels", config.url("/state/labels"))
        assertEquals("https://elsewhere/x", config.url("https://elsewhere/x"))
    }

    @Test
    fun `site pages carry the author when it is known`() {
        assertEquals("http://site.test/dashboard?username=sam+uel", config.siteDashboardUrl("sam uel"))
        assertEquals("http://site.test/dashboard", config.siteDashboardUrl(null))
        assertEquals("http://site.test/navigateto?username=sam", config.siteUploadUrl("sam"))
        assertEquals("http://site.test/api/repo-by-username?username=sam&refresh=1", config.siteUserPackagesUrl("sam", refresh = true))
        assertEquals("http://site.test/api/repo-by-username?username=sam", config.siteUserPackagesUrl("sam"))
    }

    @Test
    fun `the environment of the test task overrides the saved file`() {
        val loaded = BackendConfig.load()
        assertEquals("http://backend.test", loaded.base)
        assertEquals("http://site.test", loaded.site)
        assertEquals("http://s3.test", loaded.s3)
    }
}
