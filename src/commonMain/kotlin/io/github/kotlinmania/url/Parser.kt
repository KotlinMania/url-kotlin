// port-lint: source parser.rs
package io.github.kotlinmania.url

internal class BasicUrlParser(
    private val input: String,
    private val baseUrl: Url?,
    private val encodingOverride: EncodingOverride,
    private val violationFn: ((SyntaxViolation) -> Unit)?,
) {
    fun parse(): Result<Url> {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return if (baseUrl != null) Result.success(baseUrl) else Result.failure(UrlError.RelativeUrlWithoutBase)
        }

        // Try to parse as a full URL with scheme
        val schemeEnd = findSchemeEnd(trimmed)
        if (schemeEnd > 0) {
            return parseAbsolute(trimmed, schemeEnd)
        }

        // No scheme found - try relative URL resolution
        if (baseUrl == null) {
            return Result.failure(UrlError.RelativeUrlWithoutBase)
        }

        return resolveRelative(trimmed, baseUrl)
    }

    private fun findSchemeEnd(s: String): Int {
        if (s.isEmpty() || !s.first().isLetter()) return -1
        for (i in 1 until s.length) {
            val c = s[i]
            if (c == ':') return i
            if (!c.isLetterOrDigit() && c != '+' && c != '-' && c != '.') return -1
        }
        return -1
    }

    private fun parseAbsolute(input: String, schemeEnd: Int): Result<Url> {
        val scheme = input.substring(0, schemeEnd).lowercase()
        val rest = input.substring(schemeEnd + 1)
        val isSpecial = scheme in specialSchemes

        if (isSpecial) {
            if (rest.startsWith("//")) {
                return parseWithAuthority(scheme, rest.substring(2))
            }
            if (scheme == "file") {
                return parseWithAuthority(scheme, "/$rest")
            }
        }

        if (rest.startsWith("//")) {
            return parseWithAuthority(scheme, rest.substring(2))
        }

        return parseWithoutAuthority(scheme, rest)
    }

    private fun parseWithAuthority(scheme: String, authorityAndPath: String): Result<Url> {
        val serialization = StringBuilder()
        serialization.append(scheme).append("://")

        val schemeEnd = serialization.length - 3 // position of ':'
        val serializationInitialLen = serialization.length

        var usernameEnd = serializationInitialLen
        var hostStart = serializationInitialLen
        var hostEnd = serializationInitialLen
        var pathStart: Int
        var host: HostInternal = HostInternal.None
        var port: Int? = null
        var queryStart: Int? = null
        var fragmentStart: Int? = null

        // Find where the path/query/fragment starts after the authority
        val firstSlash = authorityAndPath.indexOf('/')
        val firstQuery = authorityAndPath.indexOf('?')
        val firstHash = authorityAndPath.indexOf('#')

        // Authority ends at the first of /, ?, #, or end of string
        // indexOf returns -1 for not found — filter those out
        val authorityEndIdx =
            when {
                firstSlash >= 0 && firstQuery >= 0 && firstHash >= 0 -> minOf(firstSlash, firstQuery, firstHash)
                firstSlash >= 0 && firstQuery >= 0 -> minOf(firstSlash, firstQuery)
                firstSlash >= 0 && firstHash >= 0 -> minOf(firstSlash, firstHash)
                firstQuery >= 0 && firstHash >= 0 -> minOf(firstQuery, firstHash)
                firstSlash >= 0 -> firstSlash
                firstQuery >= 0 -> firstQuery
                firstHash >= 0 -> firstHash
                else -> authorityAndPath.length
            }

        val authority = authorityAndPath.substring(0, authorityEndIdx)
        val restAfterAuthority = authorityAndPath.substring(authorityEndIdx)

        // Parse userinfo (username:password@)
        val atIdx = authority.indexOf('@')
        val userInfo: String? =
            if (atIdx >= 0) {
                authority.substring(0, atIdx)
            } else {
                null
            }

        var hostPart = if (atIdx >= 0) authority.substring(atIdx + 1) else authority

        if (userInfo != null) {
            if (userInfo.isNotEmpty() && userInfo != ":") {
                val colonIdx = userInfo.indexOf(':')
                if (colonIdx >= 0) {
                    // Append just the username part
                    serialization.append(userInfo.substring(0, colonIdx))
                    usernameEnd = serialization.length
                    // Append :password
                    serialization.append(':').append(userInfo.substring(colonIdx + 1))
                } else {
                    serialization.append(userInfo)
                    usernameEnd = serialization.length
                }
                serialization.append('@')
                hostStart = serialization.length
            } else {
                // Empty userinfo: skip the @ entirely
                usernameEnd = serialization.length
                hostStart = serialization.length
            }
        } else {
            usernameEnd = serialization.length
            hostStart = serialization.length
        }

        // Parse host:port from hostPart
        var portStr: String? = null
        if (hostPart.startsWith("[") && hostPart.endsWith("]")) {
            // IPv6, no port
            val inner = hostPart.substring(1, hostPart.length - 1)
            val expanded = expandIpv6(inner)
            val shortened = shortenIpv6(expanded)
            serialization.append('[').append(shortened).append(']')
            host = HostInternal.Ipv6(expanded)
        } else {
            val colonIdx = hostPart.lastIndexOf(':')
            if (colonIdx >= 0) {
                val possiblePort = hostPart.substring(colonIdx + 1)
                val p = possiblePort.toIntOrNull()
                if (p != null && p in 0..65535) {
                    port = p
                    hostPart = hostPart.substring(0, colonIdx)
                    if (port != defaultPort(scheme)) {
                        portStr = possiblePort
                    } else {
                        port = null
                    }
                }
            }
            if (hostPart.startsWith("[") && hostPart.endsWith("]")) {
                val inner = hostPart.substring(1, hostPart.length - 1)
                val expanded = expandIpv6(inner)
                val shortened = shortenIpv6(expanded)
                serialization.append('[').append(shortened).append(']')
                host = HostInternal.Ipv6(expanded)
            } else {
                val ipv4 = parseIpv4Addr(hostPart)
                if (ipv4 != null) {
                    host = HostInternal.Ipv4(ipv4)
                    serialization.append(ipv4)
                } else if (hostPart.isNotEmpty()) {
                    if (scheme in specialSchemes) {
                        if (!isValidDomain(hostPart)) {
                            return Result.failure(UrlError.InvalidDomainCharacter)
                        }
                        if (looksLikeIpv4ButInvalid(hostPart)) {
                            return Result.failure(UrlError.InvalidDomainCharacter)
                        }
                    }
                    host = HostInternal.Domain
                    serialization.append(percentPreservingLowercase(hostPart))
                }
            }
        }

        hostEnd = serialization.length

        if (portStr != null) {
            serialization.append(':').append(portStr)
        }

        pathStart = serialization.length

        // Parse path, query, fragment from restAfterAuthority
        val hashIdx = restAfterAuthority.indexOf('#')
        val qIdx =
            if (hashIdx >= 0) {
                restAfterAuthority.substring(0, hashIdx).indexOf('?')
            } else {
                restAfterAuthority.indexOf('?')
            }

        val pathStr: String =
            when {
                qIdx >= 0 -> restAfterAuthority.substring(0, qIdx)
                hashIdx >= 0 -> restAfterAuthority.substring(0, hashIdx)
                else -> restAfterAuthority
            }
        val queryStr: String? =
            if (qIdx >= 0) {
                if (hashIdx >= 0) {
                    restAfterAuthority.substring(qIdx + 1, hashIdx)
                } else {
                    restAfterAuthority.substring(qIdx + 1)
                }
            } else {
                null
            }
        val fragmentStr: String? =
            if (hashIdx >= 0) {
                restAfterAuthority.substring(hashIdx + 1)
            } else {
                null
            }

        val isSpecialScheme = scheme in specialSchemes

        if (pathStr.startsWith("/")) {
            val sp = if (needsShortening(pathStr)) shortenPath(pathStr) else pathStr
            serialization.append(sp)
        } else if (pathStr.isEmpty()) {
            if (isSpecialScheme) serialization.append('/')
        } else {
            val rawPath = "/$pathStr"
            val sp = if (needsShortening(rawPath)) shortenPath(rawPath) else rawPath
            serialization.append(sp)
        }

        if (queryStr != null) {
            queryStart = serialization.length
            serialization.append('?').append(queryStr)
        }
        if (fragmentStr != null) {
            fragmentStart = serialization.length
            serialization.append('#').append(fragmentStr)
        }

        return Result.success(
            Url(
                serialization = serialization.toString(),
                schemeEnd = schemeEnd,
                usernameEnd = usernameEnd,
                hostStart = hostStart,
                hostEnd = hostEnd,
                host = host,
                port = port,
                pathStart = pathStart,
                queryStart = queryStart,
                fragmentStart = fragmentStart,
            ),
        )
    }

    internal fun looksLikeIpv4ButInvalid(s: String): Boolean {
        val parts = s.split('.')
        if (parts.size < 2) return false
        return parts.all { part ->
            part.isNotEmpty() &&
                (
                    part.all { it.isDigit() } ||
                        part.startsWith("0x") ||
                        part.startsWith("0X")
                )
        }
    }

    internal fun needsShortening(path: String): Boolean {
        val parts = path.split('/')
        return parts.any { it == "." || it == ".." }
    }

    internal fun shortenPath(path: String): String {
        val parts = path.split('/')
        val result = mutableListOf<String>()
        var addTrailingSlash = false
        for (part in parts) {
            when (part) {
                "" -> if (result.isEmpty()) result.add("")
                "." -> addTrailingSlash = true
                ".." -> {
                    if (result.size > 1 && (result.last().length != 2 || result.last()[1] != ':')) {
                        result.removeLast()
                    } else if (result.isEmpty()) {
                        result.add("")
                    }
                    addTrailingSlash = true
                }
                else -> {
                    result.add(part)
                    addTrailingSlash = false
                }
            }
        }
        if (addTrailingSlash && result.isNotEmpty() && result.last() != "") {
            result.add("")
        }
        val joined = result.joinToString("/")
        return if (joined.isEmpty()) {
            "/"
        } else if (joined.startsWith("/")) {
            joined
        } else {
            "/$joined"
        }
    }

    private fun parseWithoutAuthority(scheme: String, rest: String): Result<Url> {
        val serialization = "$scheme:"
        val schemeEnd = scheme.length

        val hashIdx = rest.indexOf('#')
        val qIdx =
            if (hashIdx >= 0) {
                rest.substring(0, hashIdx).indexOf('?')
            } else {
                rest.indexOf('?')
            }

        val pathStr: String =
            when {
                qIdx >= 0 -> rest.substring(0, qIdx)
                hashIdx >= 0 -> rest.substring(0, hashIdx)
                else -> rest
            }
        val queryStr: String? =
            if (qIdx >= 0) {
                if (hashIdx >= 0) {
                    rest.substring(qIdx + 1, hashIdx)
                } else {
                    rest.substring(qIdx + 1)
                }
            } else {
                null
            }
        val fragmentStr: String? = if (hashIdx >= 0) rest.substring(hashIdx + 1) else null

        val serialBuf = StringBuilder(serialization)
        serialBuf.append(pathStr)

        val baseLen = serialization.length // position of path start (after "scheme:")
        val pathStart = baseLen

        val result =
            Url(
                serialization = serialBuf.toString() + (queryStr?.let { "?$it" } ?: "") + (fragmentStr?.let { "#$it" } ?: ""),
                schemeEnd = schemeEnd,
                usernameEnd = schemeEnd + 1,
                hostStart = schemeEnd + 1,
                hostEnd = schemeEnd + 1,
                host = HostInternal.None,
                port = null,
                pathStart = pathStart,
                queryStart = if (queryStr != null) serialBuf.length else null,
                fragmentStart = (serialBuf.length + if (queryStr != null) 1 + queryStr.length else 0).takeIf { fragmentStr != null },
            )

        return Result.success(result)
    }

    private fun resolveRelative(input: String, base: Url): Result<Url> {
        val schemeEnd = findSchemeEnd(input)

        if (schemeEnd > 0) {
            val scheme = input.substring(0, schemeEnd).lowercase()
            if (scheme in specialSchemes || scheme != base.scheme()) {
                return parseAbsolute(input, schemeEnd)
            }
        }

        // Scheme-relative URL (starts with //)
        if (input.startsWith("//")) {
            return parseWithAuthority(base.scheme(), input.substring(2))
        }

        // Path-absolute or path-relative
        val sb = StringBuilder()
        sb.append(base.scheme()).append(':')

        if (input.startsWith("/")) {
            // Path-absolute
            sb.append("//")
            if (base.hasAuthority()) {
                sb.append(base.serialization.substring(base.schemeEnd + 3, base.pathStart))
            }
            sb.append(input)
        } else if (input.startsWith("?")) {
            // Query-only
            sb.append("//")
            if (base.hasAuthority()) {
                sb.append(base.serialization.substring(base.schemeEnd + 3, base.pathStart))
            }
            sb.append(base.path())
            sb.append(input)
        } else if (input.startsWith("#")) {
            // Fragment-only
            sb.append("//")
            if (base.hasAuthority()) {
                sb.append(base.serialization.substring(base.schemeEnd + 3, base.pathStart))
            }
            sb.append(base.path())
            base.query()?.let { sb.append("?$it") }
            sb.append(input)
        } else {
            // Path-relative
            sb.append("//")
            if (base.hasAuthority()) {
                sb.append(base.serialization.substring(base.schemeEnd + 3, base.pathStart))
            }
            val existingPath = base.path()
            val resolvedPath = resolvePathRelative(existingPath, input)
            sb.append(resolvedPath)
        }

        val resolved = sb.toString()
        val newSchemeEnd = resolved.indexOf(':')
        return parseAbsolute(resolved, newSchemeEnd)
    }

    internal fun resolvePathRelative(basePath: String, input: String): String {
        val isWindowsDrive = basePath.startsWith("/") && basePath.length >= 3 && basePath[2] == ':'
        if (isWindowsDrive && basePath.lastIndexOf('/') == 0) {
            return "$basePath/$input"
        }
        val lastSlash = basePath.lastIndexOf('/')
        return if (lastSlash >= 0) "${basePath.substring(0, lastSlash + 1)}$input" else "/$input"
    }
}

// Lowercase ASCII letters but uppercase percent-encoded hex digits in a host string
private fun percentPreservingLowercase(s: String): String {
    val sb = StringBuilder(s.length)
    var i = 0
    while (i < s.length) {
        when {
            s[i] == '%' && i + 2 < s.length -> {
                sb.append('%')
                sb.append(s[i + 1].uppercaseChar())
                sb.append(s[i + 2].uppercaseChar())
                i += 3
            }
            else -> {
                sb.append(s[i].lowercaseChar())
                i++
            }
        }
    }
    return sb.toString()
}
