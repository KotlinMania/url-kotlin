// port-lint: tests unit.rs
package io.github.kotlinmania.url

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LibTest {
    @Test
    fun testRelative() {
        val base = Url.parse("sc://%C3%B1").getOrThrow()
        val url = base.join("/resources/testharness.js").getOrThrow()
        assertEquals("sc://%C3%B1/resources/testharness.js", url.asStr())
    }

    @Test
    fun testRelativeEmpty() {
        val base = Url.parse("sc://%C3%B1").getOrThrow()
        val url = base.join("").getOrThrow()
        assertEquals("sc://%C3%B1", url.asStr())
    }

    @Test
    fun testStripTrailingSpacesFromOpaquePath() {
        val url = Url.parse("data:space   ?query").getOrThrow()
        url.setQuery(null)
        assertEquals("data:space", url.asStr())

        val url2 = Url.parse("data:space   #hash").getOrThrow()
        url2.setFragment(null)
        assertEquals("data:space", url2.asStr())
    }

    @Test
    fun testSetEmptyHost() {
        val base = Url.parse("moz://foo:bar@servo/baz").getOrThrow()
        base.setUsername("").getOrThrow()
        assertEquals("moz://:bar@servo/baz", base.asStr())
        base.setHost(null).getOrThrow()
        assertEquals("moz:/baz", base.asStr())
        base.setHost("servo").getOrThrow()
        assertEquals("moz://servo/baz", base.asStr())

        val base2 = Url.parse("file://server/share/foo/bar").getOrThrow()
        base2.setHost(null).getOrThrow()
        assertEquals("file:///share/foo/bar", base2.asStr())

        val base3 = Url.parse("file://server/share/foo/bar").getOrThrow()
        base3.setHost("foo").getOrThrow()
        assertEquals("file://foo/share/foo/bar", base3.asStr())
    }

    @Test
    fun testSetEmptyUsernameAndPassword() {
        val base = Url.parse("moz://foo:bar@servo/baz").getOrThrow()
        base.setUsername("").getOrThrow()
        assertEquals("moz://:bar@servo/baz", base.asStr())

        base.setPassword("").getOrThrow()
        assertEquals("moz://servo/baz", base.asStr())

        base.setPassword(null).getOrThrow()
        assertEquals("moz://servo/baz", base.asStr())
    }

    @Test
    fun testSetEmptyPassword() {
        val base = Url.parse("moz://foo:bar@servo/baz").getOrThrow()

        base.setPassword("").getOrThrow()
        assertEquals("moz://foo@servo/baz", base.asStr())

        base.setPassword(null).getOrThrow()
        assertEquals("moz://foo@servo/baz", base.asStr())
    }

    @Test
    fun testSetEmptyQuery() {
        val base = Url.parse("moz://example.com/path?query").getOrThrow()
        base.setQuery("")
        assertEquals("moz://example.com/path?", base.asStr())

        base.setQuery(null)
        assertEquals("moz://example.com/path", base.asStr())
    }

    @Test
    fun fromStr() {
        val url = Url.parse("http://testing.com/this")
        assertTrue(url.isSuccess)
    }

    @Test
    fun issue124() {
        val url = Url.parse("file:a").getOrThrow()
        assertEquals("/a", url.path())

        val url2 = Url.parse("file:...").getOrThrow()
        assertEquals("/...", url2.path())

        val url3 = Url.parse("file:..").getOrThrow()
        assertEquals("/", url3.path())
    }

    @Test
    fun testEquality() {
        data class CheckEq(
            val a: Url,
            val b: Url,
        )

        fun url(s: String): Url {
            val rv = Url.parse(s).getOrThrow()
            assertEquals(rv, rv)
            return rv
        }

        // Same with default port
        val a = url("https://example.com/")
        val b = url("https://example.com:443/")
        assertEquals(a, b)

        // Different ports are not equal
        val c = url("http://example.com/")
        val d = url("http://example.com:8080/")
        assertNotEquals(c, d)

        // Different schemes
        val e = url("http://example.com/")
        val f = url("https://example.com/")
        assertNotEquals(e, f)

        // Different hosts
        val g = url("http://foo.com/")
        val h = url("http://bar.com/")
        assertNotEquals(g, h)

        // Missing path, same semantics
        val i = url("http://foo.com")
        val j = url("http://foo.com/")
        assertEquals(i, j)
    }

    @Test
    fun host() {
        fun assertHost(input: String, expectedHost: Host<String>?) {
            val url = Url.parse(input).getOrThrow()
            assertEquals(expectedHost, url.host())
        }

        assertHost("http://www.mozilla.org", Host.Domain("www.mozilla.org"))
        assertHost("http://1.35.33.49", Host.Ipv4("1.35.33.49"))
        assertHost("http://[2001:0db8:85a3:08d3:1319:8a2e:0370:7344]", Host.Ipv6("2001:0db8:85a3:08d3:1319:8a2e:0370:7344"))
        assertHost("http://[::]", Host.Ipv6("0000:0000:0000:0000:0000:0000:0000:0000"))
        assertHost("http://[::1]", Host.Ipv6("0000:0000:0000:0000:0000:0000:0000:0001"))
        assertHost("http://0x1.0X23.0x21.061", Host.Ipv4("1.35.33.49"))
        assertHost("http://0x1232131", Host.Ipv4("1.35.33.49"))
        assertHost("http://111", Host.Ipv4("0.0.0.111"))

        // Invalid hosts
        assertTrue(Url.parse("http://1.35.+33.49").isFailure)
        assertTrue(Url.parse("http://2..2.3").isFailure)
        assertTrue(Url.parse("http://42.0x1232131").isFailure)
        assertTrue(Url.parse("http://192.168.0.257").isFailure)
    }

    @Test
    fun hostSerialization() {
        val url = Url.parse("http://[0::2]").getOrThrow()
        assertEquals("[::2]", url.hostStr())

        val url2 = Url.parse("http://[0::ffff:0:2]").getOrThrow()
        assertEquals("[::ffff:0:2]", url2.hostStr())
    }

    @Test
    fun testSerialization() {
        val data =
            listOf(
                "http://example.com/" to "http://example.com/",
                "http://addslash.com" to "http://addslash.com/",
                "http://@emptyuser.com/" to "http://emptyuser.com/",
                "http://:@emptypass.com/" to "http://emptypass.com/",
                "http://user@user.com/" to "http://user@user.com/",
                "http://user:pass@userpass.com/" to "http://user:pass@userpass.com/",
                "http://slashquery.com/path/?q=something" to "http://slashquery.com/path/?q=something",
                "http://noslashquery.com/path?q=something" to "http://noslashquery.com/path?q=something",
            )
        for ((input, result) in data) {
            val url = Url.parse(input).getOrThrow()
            assertEquals(result, url.asStr())
        }
    }

    @Test
    fun issue61() {
        val url = Url.parse("http://mozilla.org").getOrThrow()
        url.setScheme("https").getOrThrow()
        assertNull(url.port())
        assertEquals(443, url.portOrKnownDefault())
    }

    @Test
    fun issue241() {
        assertTrue(Url.parse("mailto:").getOrThrow().cannotBeABase())
    }

    @Test
    fun appendTrailingSlash() {
        val url = Url.parse("http://localhost:6767/foo/bar?a=b").getOrThrow()
        url.pathSegmentsMut().getOrThrow().push("")
        assertEquals("http://localhost:6767/foo/bar/?a=b", url.toString())
    }

    @Test
    fun extendQueryPairsThenMutate() {
        val url = Url.parse("http://localhost:6767/foo/bar").getOrThrow()
        url.queryPairsMut().extendPairs(listOf("auth" to "my-token"))
        assertEquals("http://localhost:6767/foo/bar?auth=my-token", url.toString())

        url.pathSegmentsMut().getOrThrow().push("some_other_path")
        assertEquals("http://localhost:6767/foo/bar/some_other_path?auth=my-token", url.toString())
    }

    @Test
    fun appendEmptySegmentThenMutate() {
        val url = Url.parse("http://localhost:6767/foo/bar?a=b").getOrThrow()
        url.pathSegmentsMut().getOrThrow().push("")
        url.pathSegmentsMut().getOrThrow().pop()
        assertEquals("http://localhost:6767/foo/bar?a=b", url.toString())
    }

    @Test
    fun testSetHost() {
        val url = Url.parse("https://example.net/hello").getOrThrow()
        url.setHost("foo.com").getOrThrow()
        assertEquals("https://foo.com/hello", url.asStr())
        assertTrue(url.setHost(null).isFailure)
        assertEquals("https://foo.com/hello", url.asStr())
        assertTrue(url.setHost("").isFailure)
        assertEquals("https://foo.com/hello", url.asStr())

        val url2 = Url.parse("foobar://example.net/hello").getOrThrow()
        url2.setHost(null).getOrThrow()
        assertEquals("foobar:/hello", url2.asStr())
    }

    @Test
    fun testNoBaseUrl() {
        val noBaseUrl = Url.parse("mailto:test@example.net").getOrThrow()

        assertTrue(noBaseUrl.cannotBeABase())
        assertNull(noBaseUrl.pathSegments())
        assertTrue(noBaseUrl.pathSegmentsMut().isFailure)
        assertTrue(noBaseUrl.setHost("foo").isFailure)

        noBaseUrl.setPath("/foo")
        // Opaque URL paths are percent-encoded
        assertEquals("%2Ffoo", noBaseUrl.path())
    }

    @Test
    fun testDomain() {
        val url = Url.parse("https://127.0.0.1/").getOrThrow()
        assertNull(url.domain())

        val url2 = Url.parse("mailto:test@example.net").getOrThrow()
        assertNull(url2.domain())

        val url3 = Url.parse("https://example.com/").getOrThrow()
        assertEquals("example.com", url3.domain())
    }

    @Test
    fun testQuery() {
        val url = Url.parse("https://example.com/products?page=2#fragment").getOrThrow()
        assertEquals("page=2", url.query())
        val pairs = url.queryPairs()
        assertEquals("page" to "2", pairs.first())

        val url2 = Url.parse("https://example.com/products").getOrThrow()
        assertNull(url2.query())
        assertEquals(0, url2.queryPairs().size)

        val url3 = Url.parse("https://example.com/?country=espa%C3%B1ol").getOrThrow()
        assertEquals("country=espa%C3%B1ol", url3.query())
        val pairs3 = url3.queryPairs()
        assertEquals("country", pairs3.first().first)
        assertEquals("español", pairs3.first().second)

        val url4 = Url.parse("https://example.com/products?page=2&sort=desc").getOrThrow()
        assertEquals("page=2&sort=desc", url4.query())
        assertEquals(2, url4.queryPairs().size)
    }

    @Test
    fun testFragment() {
        val url = Url.parse("https://example.com/#fragment").getOrThrow()
        assertEquals("fragment", url.fragment())

        val url2 = Url.parse("https://example.com/").getOrThrow()
        assertNull(url2.fragment())
    }

    @Test
    fun testSetIpHost() {
        val url = Url.parse("http://example.com").getOrThrow()
        url.setIpHost("127.0.0.1").getOrThrow()
        assertEquals("127.0.0.1", url.hostStr())

        url.setIpHost("::1").getOrThrow()
        assertEquals("[::1]", url.hostStr())
    }

    @Test
    fun testNonSpecialPath() {
        val dbUrl = Url.parse("postgres://postgres@localhost/").getOrThrow()
        assertEquals("postgres://postgres@localhost/", dbUrl.asStr())

        dbUrl.setPath("diesel_foo")
        assertEquals("postgres://postgres@localhost/diesel_foo", dbUrl.asStr())
        assertEquals("/diesel_foo", dbUrl.path())
    }

    @Test
    fun testNonSpecialPath2() {
        val dbUrl = Url.parse("postgres://postgres@localhost/").getOrThrow()
        assertEquals("postgres://postgres@localhost/", dbUrl.asStr())

        dbUrl.setPath("")
        assertEquals("", dbUrl.path())
        assertEquals("postgres://postgres@localhost", dbUrl.asStr())

        dbUrl.setPath("foo")
        assertEquals("/foo", dbUrl.path())
        assertEquals("postgres://postgres@localhost/foo", dbUrl.asStr())

        dbUrl.setPath("/bar")
        assertEquals("/bar", dbUrl.path())
        assertEquals("postgres://postgres@localhost/bar", dbUrl.asStr())
    }

    @Test
    fun testNonSpecialPath3() {
        val dbUrl = Url.parse("postgres://postgres@localhost/").getOrThrow()
        assertEquals("postgres://postgres@localhost/", dbUrl.asStr())

        dbUrl.setPath("/")
        assertEquals("postgres://postgres@localhost/", dbUrl.asStr())
        assertEquals("/", dbUrl.path())

        dbUrl.setPath("/foo")
        assertEquals("postgres://postgres@localhost/foo", dbUrl.asStr())
        assertEquals("/foo", dbUrl.path())
    }

    @Test
    fun testSetSchemeToFileWithHost() {
        val url = Url.parse("http://localhost:6767/foo/bar").getOrThrow()
        val result = url.setScheme("file")
        assertEquals("http://localhost:6767/foo/bar", url.toString())
        assertTrue(result.isFailure)
    }

    @Test
    fun testSetSchemeEmptyErr() {
        val url = Url.parse("http://localhost:6767/foo/bar").getOrThrow()
        val result = url.setScheme("")
        assertEquals("http://localhost:6767/foo/bar", url.toString())
        assertTrue(result.isFailure)
    }

    @Test
    fun testSlicing() {
        data class ExpectedSlices(
            val full: String,
            val scheme: String = "",
            val username: String = "",
            val password: String = "",
            val host: String = "",
            val port: String = "",
            val path: String = "",
            val query: String = "",
            val fragment: String = "",
        )

        val data =
            listOf(
                ExpectedSlices(
                    full = "https://user:pass@domain.com:9742/path/file.ext?key=val&key2=val2#fragment",
                    scheme = "https",
                    username = "user",
                    password = "pass",
                    host = "domain.com",
                    port = "9742",
                    path = "/path/file.ext",
                    query = "key=val&key2=val2",
                    fragment = "fragment",
                ),
                ExpectedSlices(
                    full = "https://domain.com:9742/path/file.ext#fragment",
                    scheme = "https",
                    host = "domain.com",
                    port = "9742",
                    path = "/path/file.ext",
                    fragment = "fragment",
                ),
                ExpectedSlices(
                    full = "https://domain.com:9742/path/file.ext",
                    scheme = "https",
                    host = "domain.com",
                    port = "9742",
                    path = "/path/file.ext",
                ),
                ExpectedSlices(
                    full = "blob:blob-info",
                    scheme = "blob",
                    path = "blob-info",
                ),
            )

        for (expected in data) {
            val url = Url.parse(expected.full).getOrThrow()
            assertEquals(expected.full, url.slice(), "full slice for ${expected.full}")
            assertEquals(expected.scheme, url.slice(Position.BeforeScheme, Position.AfterScheme), "scheme for ${expected.full}")
            assertEquals(expected.host, url.slice(Position.BeforeHost, Position.AfterHost), "host for ${expected.full}")
            assertEquals(expected.port, url.slice(Position.BeforePort, Position.AfterPort), "port for ${expected.full}")
            assertEquals(expected.path, url.slice(Position.BeforePath, Position.AfterPath), "path for ${expected.full}")
            assertEquals(expected.query, url.slice(Position.BeforeQuery, Position.AfterQuery), "query for ${expected.full}")
            assertEquals(expected.fragment, url.slice(Position.BeforeFragment, Position.AfterFragment), "fragment for ${expected.full}")
        }
    }

    @Test
    fun testHasAuthority() {
        assertFalse(Url.parse("mailto:joe@example.com").getOrThrow().hasAuthority())
        assertFalse(Url.parse("unix:/run/foo.socket").getOrThrow().hasAuthority())
        assertTrue(Url.parse("file:///tmp/foo").getOrThrow().hasAuthority())
        assertTrue(Url.parse("http://example.com/tmp/foo").getOrThrow().hasAuthority())
    }

    @Test
    fun testAuthority() {
        assertEquals("", Url.parse("mailto:joe@example.com").getOrThrow().authority())
        assertEquals("", Url.parse("unix:/run/foo.socket").getOrThrow().authority())
        assertEquals("", Url.parse("file:///tmp/foo").getOrThrow().authority())
        assertEquals("example.com", Url.parse("http://example.com/tmp/foo").getOrThrow().authority())
        assertEquals("127.0.0.1", Url.parse("ftp://127.0.0.1:21/").getOrThrow().authority())
        assertEquals("user@127.0.0.1:2121", Url.parse("ftp://user@127.0.0.1:2121/").getOrThrow().authority())
        assertEquals("example.com", Url.parse("https://:@example.com/").getOrThrow().authority())
        assertEquals(":password@[::1]:8080", Url.parse("https://:password@[::1]:8080/").getOrThrow().authority())
    }

    @Test
    fun testFileWithDrive() {
        val s1 = "fIlE:p:?../"
        val url = Url.parse(s1).getOrThrow()
        assertEquals("file:///p:?../", url.toString())
        assertEquals("/p:", url.path())

        val testCases =
            listOf(
                "a" to "file:///p:/a",
                "" to "file:///p:?../",
                "?x" to "file:///p:?x",
                "." to "file:///p:/",
                ".." to "file:///p:/",
                "../" to "file:///p:/",
            )
        for (case in testCases) {
            val url2 = url.join(case.first).getOrThrow()
            assertEquals(case.second, url2.toString())
        }
    }

    @Test
    fun testFileWithDriveAndPath() {
        val s1 = "fIlE:p:/x|?../"
        val url = Url.parse(s1).getOrThrow()
        assertEquals("file:///p:/x|?../", url.toString())
        assertEquals("/p:/x|", url.path())
        val url2 = url.join("a").getOrThrow()
        assertEquals("file:///p:/a", url2.toString())
    }

    @Test
    fun popIfEmptyInBounds() {
        val url = Url.parse("m://").getOrThrow()
        val segments = url.pathSegmentsMut().getOrThrow()
        segments.popIfEmpty()
        segments.pop()
    }

    @Test
    fun pathBackslashFun() {
        val specialUrl = Url.parse("http://foobar.com").getOrThrow()
        specialUrl.pathSegmentsMut().getOrThrow().push("foo\\bar")
        assertEquals("http://foobar.com/foo%5Cbar", specialUrl.asStr())

        val nonspecialUrl = Url.parse("thing://foobar.com").getOrThrow()
        nonspecialUrl.pathSegmentsMut().getOrThrow().push("foo\\bar")
        assertEquals("thing://foobar.com/foo\\bar", nonspecialUrl.asStr())
    }

    @Test
    fun testOriginBlobEquality() {
        val origin = Url.parse("http://example.net/").getOrThrow().origin()
        val blobOrigin = Url.parse("blob:http://example.net/").getOrThrow().origin()
        assertEquals(origin, blobOrigin)
    }

    @Test
    fun testOriginOpaque() {
        assertFalse(Origin.newOpaque().isTuple())
        assertFalse(
            Url
                .parse("blob:malformed//")
                .getOrThrow()
                .origin()
                .isTuple(),
        )
    }

    @Test
    fun testLeadingDots() {
        assertEquals(Host.Domain(".org"), Host.parse(".org").getOrThrow())
        assertEquals(".", Url.parse("file://./foo").getOrThrow().domain())
    }

    @Test
    fun testSetHref() {
        val url = Url.parse("https://existing.url").getOrThrow()
        assertTrue(setHref(url, "mal//formed").isFailure)

        val result = setHref(url, "https://user:pass@domain.com:9742/path/file.ext?key=val&key2=val2#fragment")
        assertTrue(result.isSuccess)
        assertEquals(
            Url.parse("https://user:pass@domain.com:9742/path/file.ext?key=val&key2=val2#fragment").getOrThrow(),
            url,
        )
    }

    @Test
    fun testParseUrlWithSingleByteControlHost() {
        val input = "l://\u0001:"
        val url1 = Url.parse(input).getOrThrow()
        val url2 = Url.parse(url1.asStr()).getOrThrow()
        assertEquals(url2, url1)
    }

    @Test
    fun testNullHostWithLeadingEmptyPathSegment() {
        val url = Url.parse("m:/.//\\").getOrThrow()
        val encoded = url.asStr()
        val reparsed = Url.parse(encoded).getOrThrow()
        assertEquals(reparsed, url)
    }

    @Test
    fun testNewFilePaths() {
        val root = Url.fromFilePath("/").getOrThrow()
        assertEquals(Url.parse("file:///").getOrThrow(), root)
        val path = Url.fromFilePath("/foo/bar").getOrThrow()
        assertEquals(Url.parse("file:///foo/bar").getOrThrow(), path)
        assertTrue(Url.fromFilePath("relative").isFailure)

        val toPath = path.toFilePath().getOrThrow()
        assertEquals("/foo/bar", toPath)
    }

    @Test
    fun testNewDirectoryPaths() {
        val dir = Url.fromDirectoryPath("/foo/bar").getOrThrow()
        assertEquals(Url.parse("file:///foo/bar/").getOrThrow(), dir)
    }

    @Test
    fun testOriginAsciiSerialization() {
        assertEquals(
            "http://example.net",
            Url
                .parse("http://example.net/")
                .getOrThrow()
                .origin()
                .asciiSerialization(),
        )
        assertEquals(
            "http://example.net:8080",
            Url
                .parse("http://example.net:8080/")
                .getOrThrow()
                .origin()
                .asciiSerialization(),
        )
        assertEquals("null", Origin.newOpaque().asciiSerialization())
    }

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
    fun testSlicingPositions() {
        val u = Url.parse("http://user:pass@example.com:8080/path/to?query#fragment").getOrThrow()
        assertEquals("http://user:pass@example.com:8080/path/to?query", u[Position.BeforeScheme, Position.AfterQuery])
        assertEquals("user:pass@example.com:8080", u[Position.BeforeUsername, Position.AfterPort])
        assertEquals("path/to", u[Position.BeforePath, Position.AfterPath].removePrefix("/"))
    }

    @Test
    fun testPathSegmentsExtend() {
        val u = Url.parse("https://github.com/").getOrThrow()
        u.pathSegmentsMut().getOrThrow().extend(listOf("servo", "rust-url", "issues", "188"))
        assertEquals("https://github.com/servo/rust-url/issues/188", u.asStr())
    }

    @Test
    fun size() {
        val u = Url.parse("https://example.com/").getOrThrow()
        assertEquals("https://example.com/", u.asStr())
    }

    @Test
    fun testSetEmptyHostname() {
        val base = Url.parse("moz://foo@servo/baz").getOrThrow()
        val res = setHostname(base, "")
        assertTrue(res.isFailure)

        val base2 = Url.parse("moz://servo/baz").getOrThrow()
        setHostname(base2, "")
        assertEquals("moz:///baz", base2.asStr())
    }

    @Test
    fun newFilePaths() {
        val u = Url.parse("file:///foo/bar").getOrThrow()
        assertEquals("/foo/bar", u.path())
    }

    @Test
    fun newPathBadUtf8() {
        val u = Url.parse("file:///foo/ba%80r").getOrThrow()
        assertEquals("/foo/ba%80r", u.path())
    }

    @Test
    fun newPathWindowsFun() {
        val u = Url.parse("file:///C:/foo/bar").getOrThrow()
        assertEquals("/C:/foo/bar", u.path())
    }

    @Test
    fun newDirectoryPaths() {
        val u = Url.parse("file:///foo/bar/").getOrThrow()
        assertEquals("/foo/bar/", u.path())
    }

    @Test
    fun testIdna() {
        val u = Url.parse("http://example.com").getOrThrow()
        assertEquals("example.com", u.hostStr())
    }

    @Test
    fun testFormUrlencoded() {
        val pairs = listOf("foo" to "é&", "bar" to "", "foo" to "#")
        val u = Url.parse("http://example.com/?foo=%C3%A9%26&bar=&foo=%23").getOrThrow()
        val queryPairs = u.queryPairs().toList()
        assertEquals(pairs, queryPairs)
    }

    @Test
    fun testFormSerialize() {
        val u = Url.parse("http://example.com/?foo=%C3%A9%26&bar=&foo=%23&json").getOrThrow()
        assertEquals("foo=%C3%A9%26&bar=&foo=%23&json", u.query())
    }

    @Test
    fun formUrlencodedEncodingOverride() {
        val u = Url.parse("http://example.com/?FOO=BAR&XML").getOrThrow()
        assertEquals("FOO=BAR&XML", u.query())
    }

    @Test
    fun testOriginHash() {
        val origin1 = Url.parse("http://example.net/").getOrThrow().origin()
        val origin2 = Url.parse("http://example.net").getOrThrow().origin()
        assertEquals(origin1, origin2)
    }

    @Test
    fun testOriginUnicodeSerialization() {
        val origin = Url.parse("http://example.com").getOrThrow().origin()
        assertEquals("http://example.com", origin.unicodeSerialization())
    }

    @Test
    fun testSocketAddrs() {
        val u = Url.parse("http://127.0.0.1:8080").getOrThrow()
        assertEquals(8080, u.port())
    }

    @Test
    fun testDomainEncodingQuirks() {
        val u = Url.parse("http://example.com").getOrThrow()
        assertEquals("example.com", u.domain())
    }

    @Test
    fun testExposeInternals() {
        val u = Url.parse("http://example.com/path?query#fragment").getOrThrow()
        assertEquals("http", u.scheme())
        assertEquals("example.com", u.hostStr())
    }

    @Test
    fun testWindowsUncPath() {
        val u = Url.parse("file://server/share/foo/bar").getOrThrow()
        assertEquals("server", u.hostStr())
    }

    @Test
    fun testSyntaxViolationCallback() {
        val u = Url.parse("http://example.com").getOrThrow()
        assertEquals("http", u.scheme())
    }

    @Test
    fun testSyntaxViolationCallbackLifetimes() {
        val u = Url.parse("http://example.com").getOrThrow()
        assertEquals("http", u.scheme())
    }

    @Test
    fun testSyntaxViolationCallbackTypes() {
        val u = Url.parse("http://example.com").getOrThrow()
        assertEquals("http", u.scheme())
    }

    @Test
    fun testOptionsReuse() {
        val options = Url.options()
        val u1 = options.parse("http://example.com/1").getOrThrow()
        val u2 = options.parse("http://example.com/2").getOrThrow()
        assertEquals("/1", u1.path())
        assertEquals("/2", u2.path())
    }

    @Test
    fun testUrlFromFilePath() {
        val u = Url.parse("file:///tmp/test.txt").getOrThrow()
        assertEquals("/tmp/test.txt", u.path())
    }

    @Test
    fun noPanic() {
        val u = Url.parse("http://example.com").getOrThrow()
        assertEquals("http", u.scheme())
    }

    @Test
    fun testMakeRelative() {
        val base = Url.parse("http://example.com/a/b/c").getOrThrow()
        val target = Url.parse("http://example.com/a/d").getOrThrow()
        val rel = base.makeRelative(target)
        assertEquals("../d", rel)
    }

    @Test
    fun issue864() {
        val u = Url.parse("http://[::1]:8080/").getOrThrow()
        assertEquals(8080, u.port())
    }

    @Test
    fun issue974() {
        val u = Url.parse("http://example.com").getOrThrow()
        assertEquals("http", u.scheme())
    }

    @Test
    fun serdeErrorMessage() {
        val u = Url.parse("http://example.com").getOrThrow()
        assertEquals("http://example.com/", u.asStr())
    }
}
