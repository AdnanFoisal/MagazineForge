package com.magazineforge.app.ui

import org.junit.Assert.assertEquals
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
}
