package com.magazineforge.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MyMagazinesScreenTest {

    @Test
    fun testToTopicSegment() {
        assertEquals("hello_world", toTopicSegment("hello world"))
        assertEquals("hello-world", toTopicSegment("hello-world"))
        assertEquals("hello_world", toTopicSegment("  hello   world  "))
        assertEquals("hello_world", toTopicSegment("hello_world"))
        assertEquals("hello_world", toTopicSegment("hello.world"))
        assertEquals("hello_world", toTopicSegment("hello @ world"))
        assertEquals("a_b_c", toTopicSegment("a b c"))
        assertEquals("test_123", toTopicSegment("test 123"))
        assertEquals("test_123", toTopicSegment("test/123"))
        assertEquals("hello_world", toTopicSegment("_hello_world_"))
        assertEquals("hello-world", toTopicSegment("-hello-world-"))
    }

    @Test
    fun normalizesSupportedTemplateVariantsWithoutThrowing() {
        assertEquals("a", normalizeTemplateVariant("cover_template_a"))
        assertEquals("b", normalizeTemplateVariant("B"))
        assertEquals("c", normalizeTemplateVariant("cover_template_c"))
        assertEquals("auto", normalizeTemplateVariant("cover_template_auto"))
        assertNull(normalizeTemplateVariant("cover_template_unknown"))
    }
}
