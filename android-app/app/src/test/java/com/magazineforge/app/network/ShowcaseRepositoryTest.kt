package com.magazineforge.app.network

import org.junit.Assert.assertEquals
import org.junit.Test

class ShowcaseRepositoryTest {

    @Test
    fun testResolveApiUrl() {
        assertEquals("", resolveApiUrl(""))
        assertEquals("http://example.com/foo", resolveApiUrl("http://example.com/foo"))
        assertEquals("https://example.com/foo", resolveApiUrl("https://example.com/foo"))
        assertEquals("https://example.com/foo", resolveApiUrl("//example.com/foo"))
        
        // Relative paths should resolve against ApiClient.BASE_URL
        val baseUrl = ApiClient.BASE_URL.trimEnd('/')
        assertEquals("$baseUrl/foo/bar", resolveApiUrl("/foo/bar"))
        assertEquals("$baseUrl/foo/bar", resolveApiUrl("foo/bar"))
    }
}
