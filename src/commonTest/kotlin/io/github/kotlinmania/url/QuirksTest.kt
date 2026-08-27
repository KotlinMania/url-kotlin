// port-lint: tests url/src/quirks.rs
package io.github.kotlinmania.url

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuirksTest {
    @Test
    fun testIsSpecialScheme() {
        assertTrue(Quirks.isSpecialScheme("http"))
        assertTrue(Quirks.isSpecialScheme("https"))
        assertTrue(Quirks.isSpecialScheme("ws"))
        assertTrue(Quirks.isSpecialScheme("wss"))
        assertTrue(Quirks.isSpecialScheme("ftp"))
        assertTrue(Quirks.isSpecialScheme("file"))
        assertFalse(Quirks.isSpecialScheme("mailto"))
        assertFalse(Quirks.isSpecialScheme("custom"))
    }

    @Test
    fun testDomainToAsciiAndUnicode() {
        assertEquals("example.com", domainToAscii("example.com"))
        assertEquals("example.com", domainToUnicode("example.com"))
    }
}
