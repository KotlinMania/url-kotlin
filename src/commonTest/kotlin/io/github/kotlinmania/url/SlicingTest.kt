// port-lint: tests url/src/slicing.rs
package io.github.kotlinmania.url

import kotlin.test.Test
import kotlin.test.assertEquals

class SlicingTest {
    @Test
    fun testCountDigits() {
        assertEquals(1, countDigits(0))
        assertEquals(1, countDigits(1))
        assertEquals(1, countDigits(9))
        assertEquals(2, countDigits(10))
        assertEquals(2, countDigits(99))
        assertEquals(3, countDigits(100))
        assertEquals(4, countDigits(9999))
        assertEquals(5, countDigits(65535))
    }

    @Test
    fun testSlicing() {
        val url = Url.parse("https://user:pass@domain.com:9742/path/file.ext?key=val&key2=val2#fragment").getOrThrow()
        assertEquals("https", url[Position.BeforeScheme, Position.AfterScheme])
        assertEquals("user", url[Position.BeforeUsername, Position.AfterUsername])
        assertEquals("pass", url[Position.BeforePassword, Position.AfterPassword])
        assertEquals("domain.com", url[Position.BeforeHost, Position.AfterHost])
        assertEquals("9742", url[Position.BeforePort, Position.AfterPort])
        assertEquals("/path/file.ext", url[Position.BeforePath, Position.AfterPath])
        assertEquals("key=val&key2=val2", url[Position.BeforeQuery, Position.AfterQuery])
        assertEquals("fragment", url[Position.BeforeFragment, Position.AfterFragment])
        assertEquals("https://user:pass@domain.com:9742/path/file.ext?key=val&key2=val2#fragment", url[Position.BeforeScheme, Position.AfterFragment])
    }
}
