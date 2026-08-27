// port-lint: source url/src/lib.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.url

import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.native.HiddenFromObjC

@HiddenFromObjC
public class ParseOptions(
    internal val baseUrl: Url? = null,
    internal val encodingOverride: EncodingOverride = null,
    internal val violationFn: ((SyntaxViolation) -> Unit)? = null,
) {
    public fun baseUrl(new: Url?): ParseOptions = ParseOptions(new, encodingOverride, violationFn)

    public fun encodingOverride(new: EncodingOverride): ParseOptions =
        ParseOptions(baseUrl, new, violationFn)

    public fun syntaxViolationCallback(new: ((SyntaxViolation) -> Unit)?): ParseOptions =
        ParseOptions(baseUrl, encodingOverride, new)

    public fun parse(input: String): Result<Url> {
        val parser =
            Parser(
                baseUrl = baseUrl,
                queryEncodingOverride = encodingOverride,
                violationFn = violationFn,
                context = Context.UrlParser,
            )
        return parser.parseUrl(input)
    }
}

@HiddenFromObjC
public class Url
    @PublishedApi
    internal constructor(
        @PublishedApi internal var serialization: String,
        @PublishedApi internal var schemeEnd: Int,
        @PublishedApi internal var usernameEnd: Int,
        @PublishedApi internal var hostStart: Int,
        @PublishedApi internal var hostEnd: Int,
        @PublishedApi internal var host: HostInternal,
        @PublishedApi internal var port: Int?,
        @PublishedApi internal var pathStart: Int,
        @PublishedApi internal var queryStart: Int?,
        @PublishedApi internal var fragmentStart: Int?,
    ) {
        public companion object {
            public fun parse(input: String): Result<Url> = options().parse(input)

            public fun parseWithParams(
                input: String,
                pairs: List<Pair<String, String>>,
            ): Result<Url> {
                val url = options().parse(input)
                if (url.isSuccess) {
                    url.getOrThrow().queryPairsMut().extendPairs(pairs)
                }
                return url
            }

            public fun options(): ParseOptions = ParseOptions()

            public fun fromFilePath(path: String): Result<Url> = pathToFileUrl(path)

            public fun fromDirectoryPath(path: String): Result<Url> {
                val url = fromFilePath(path)
                if (url.isSuccess && !url.getOrThrow().serialization.endsWith("/")) {
                    val u = url.getOrThrow()
                    u.serialization += "/"
                    return Result.success(u)
                }
                return url
            }
        }

        public fun join(input: String): Result<Url> =
            ParseOptions(baseUrl = this).parse(input)

        public fun makeRelative(url: Url): String? {
            if (cannotBeABase()) return null
            if (scheme() != url.scheme() || host() != url.host() || port() != url.port()) return null

            val relative = StringBuilder()

            fun extractPathFilename(s: String): Pair<String, String> {
                val lastSlash = s.lastIndexOf('/')
                return if (lastSlash < 0) {
                    "" to s
                } else {
                    s.substring(0, lastSlash) to s.substring(lastSlash + 1)
                }
            }

            val (basePath, baseFilename) = extractPathFilename(path())
            val (urlPath, urlFilename) = extractPathFilename(url.path())

            val baseParts = basePath.split('/').dropLastWhile { it.isEmpty() }.toMutableList()
            val urlParts = urlPath.split('/').dropLastWhile { it.isEmpty() }.toMutableList()

            // Skip common prefix
            var i = 0
            while (i < baseParts.size && i < urlParts.size && baseParts[i] == urlParts[i]) {
                i++
            }

            // Add ".." for remaining base path segments
            for (j in i until baseParts.size) {
                if (baseParts[j].isEmpty()) break
                if (relative.isNotEmpty()) relative.append('/')
                relative.append("..")
            }

            // Append remaining url path segments
            for (j in i until urlParts.size) {
                if (relative.isNotEmpty()) relative.append('/')
                relative.append(urlParts[j])
            }

            // Add filename
            if (relative.isNotEmpty() || baseFilename != urlFilename) {
                if (urlFilename.isEmpty()) {
                    relative.append('/')
                } else {
                    if (relative.isNotEmpty()) relative.append('/')
                    relative.append(urlFilename)
                }
            }

            // Query and fragment from the other URL
            url.query()?.let { relative.append('?').append(it) }
            url.fragment()?.let { relative.append('#').append(it) }

            return relative.toString()
        }

        public fun asStr(): String = serialization

        public fun slice(): String = serialization

        public fun slice(from: Position, to: Position): String {
            val start =
                when (from) {
                    Position.BeforeScheme -> 0
                    Position.AfterScheme -> schemeEnd
                    Position.BeforeUsername -> if (hasAuthority()) schemeEnd + 3 else schemeEnd + 1
                    Position.AfterUsername -> usernameEnd
                    Position.BeforePassword -> if (password() != null) usernameEnd + 1 else usernameEnd
                    Position.AfterPassword -> if (password() != null) hostStart - 1 else hostStart
                    Position.BeforeHost -> hostStart
                    Position.AfterHost -> hostEnd
                    Position.BeforePort ->
                        if (port != null) {
                            val colonIdx = serialization.indexOf(':', hostEnd)
                            if (colonIdx >= 0) colonIdx + 1 else hostEnd
                        } else {
                            hostEnd
                        }
                    Position.AfterPort -> {
                        val colonIdx = serialization.indexOf(':', hostEnd)
                        if (colonIdx >= 0) {
                            val afterColon = colonIdx + 1
                            val slashIdx = serialization.indexOf('/', afterColon).let { if (it < 0) serialization.length else it }
                            val qIdx = serialization.indexOf('?', afterColon).let { if (it < 0) serialization.length else it }
                            val hashIdx = serialization.indexOf('#', afterColon).let { if (it < 0) serialization.length else it }
                            minOf(slashIdx, qIdx, hashIdx)
                        } else {
                            hostEnd
                        }
                    }
                    Position.BeforePath -> pathStart
                    Position.AfterPath -> {
                        val qs = queryStart
                        val fs = fragmentStart
                        if (qs != null) {
                            qs
                        } else if (fs != null) {
                            fs
                        } else {
                            serialization.length
                        }
                    }
                    Position.BeforeQuery -> queryStart?.let { it + 1 } ?: (fragmentStart ?: serialization.length)
                    Position.AfterQuery -> fragmentStart ?: serialization.length
                    Position.BeforeFragment -> fragmentStart?.let { it + 1 } ?: serialization.length
                    Position.AfterFragment -> serialization.length
                }
            val end =
                when (to) {
                    Position.BeforeScheme -> 0
                    Position.AfterScheme -> schemeEnd
                    Position.BeforeUsername -> if (hasAuthority()) schemeEnd + 3 else schemeEnd + 1
                    Position.AfterUsername -> usernameEnd
                    Position.BeforePassword -> if (password() != null) usernameEnd + 1 else usernameEnd
                    Position.AfterPassword -> if (password() != null) hostStart - 1 else hostStart
                    Position.BeforeHost -> hostStart
                    Position.AfterHost -> hostEnd
                    Position.BeforePort ->
                        if (port != null) {
                            val colonIdx = serialization.indexOf(':', hostEnd)
                            if (colonIdx >= 0) colonIdx + 1 else hostEnd
                        } else {
                            hostEnd
                        }
                    Position.AfterPort -> {
                        val colonIdx = serialization.indexOf(':', hostEnd)
                        if (colonIdx >= 0) {
                            val afterColon = colonIdx + 1
                            val slashIdx = serialization.indexOf('/', afterColon).let { if (it < 0) serialization.length else it }
                            val qIdx = serialization.indexOf('?', afterColon).let { if (it < 0) serialization.length else it }
                            val hashIdx = serialization.indexOf('#', afterColon).let { if (it < 0) serialization.length else it }
                            minOf(slashIdx, qIdx, hashIdx)
                        } else {
                            hostEnd
                        }
                    }
                    Position.BeforePath -> pathStart
                    Position.AfterPath -> {
                        val qs = queryStart
                        val fs = fragmentStart
                        if (qs != null) {
                            qs
                        } else if (fs != null) {
                            fs
                        } else {
                            serialization.length
                        }
                    }
                    Position.BeforeQuery -> queryStart?.let { it + 1 } ?: (fragmentStart ?: serialization.length)
                    Position.AfterQuery -> fragmentStart ?: serialization.length
                    Position.BeforeFragment -> fragmentStart?.let { it + 1 } ?: serialization.length
                    Position.AfterFragment -> serialization.length
                }
            return serialization.substring(start, end)
        }

        public fun authority(): String {
            if (!hasAuthority()) return ""
            return serialization.substring(schemeEnd + 3, pathStart)
        }

        public fun checkInvariants(): String? {
            if (schemeEnd < 1) return "scheme_end < 1 for URL $serialization"
            if (!serialization.first().isLetter()) return "first char not alphabetic for URL $serialization"
            // Check scheme characters are valid
            val schemeChars = serialization.substring(1, schemeEnd)
            for (c in schemeChars) {
                if (!c.isLetterOrDigit() && c != '+' && c != '-' && c != '.') {
                    return "invalid scheme character '$c' for URL $serialization"
                }
            }
            if (serialization[schemeEnd] != ':') return "no ':' after scheme for URL $serialization"
            return null
        }

        public fun origin(): Origin = urlOrigin(this)

        public fun toFilePath(): Result<String> = fileUrlToPath(this)

        public fun scheme(): String = serialization.substring(0, schemeEnd)

        public fun isSpecial(): Boolean =
            scheme().lowercase() in specialSchemes

        public fun hasAuthority(): Boolean =
            serialization.length > schemeEnd && serialization.substring(schemeEnd).startsWith("://")

        public fun cannotBeABase(): Boolean =
            serialization.length <= schemeEnd + 1 || serialization[schemeEnd + 1] != '/'

        public fun username(): String {
            val sepLen = "://".length
            return if (hasAuthority() && usernameEnd > schemeEnd + sepLen) {
                serialization.substring(schemeEnd + sepLen, usernameEnd)
            } else {
                ""
            }
        }

        public fun password(): String? {
            if (hasAuthority() && usernameEnd < serialization.length && serialization[usernameEnd] == ':') {
                return serialization.substring(usernameEnd + 1, hostStart - 1)
            }
            return null
        }

        public fun hasHost(): Boolean = host != HostInternal.None

        public fun hostStr(): String? =
            if (hasHost()) serialization.substring(hostStart, hostEnd) else null

        public fun host(): Host<String>? =
            when (host) {
                HostInternal.None -> null
                HostInternal.Domain -> Host.Domain(serialization.substring(hostStart, hostEnd))
                is HostInternal.Ipv4 -> Host.Ipv4((host as HostInternal.Ipv4).address)
                is HostInternal.Ipv6 -> Host.Ipv6((host as HostInternal.Ipv6).address)
            }

        public fun byteAt(index: Int): Byte? {
            if (index < 0 || index >= serialization.length) return null
            return serialization[index].code.toByte()
        }

        public fun domain(): String? =
            when (host) {
                HostInternal.Domain -> serialization.substring(hostStart, hostEnd)
                else -> null
            }

        public fun port(): Int? = port

        public fun portOrKnownDefault(): Int? =
            port ?: defaultPort(scheme())

        public fun path(): String {
            val qs = queryStart
            val fs = fragmentStart
            val ps = pathStart
            return when {
                qs != null -> serialization.substring(ps, qs)
                fs != null -> serialization.substring(ps, fs)
                else -> serialization.substring(ps)
            }
        }

        public fun pathSegments(): List<String>? {
            val p = path()
            return if (p.startsWith("/")) {
                p.substring(1).split('/').filter { it.isNotEmpty() || p != "/" }
            } else {
                null
            }
        }

        public fun query(): String? {
            val qs = queryStart
            val fs = fragmentStart
            return when {
                qs == null -> null
                fs == null -> serialization.substring(qs + 1)
                else -> serialization.substring(qs + 1, fs)
            }
        }

        public fun queryPairs(): List<Pair<String, String>> {
            val q = query() ?: return emptyList()
            return parseFormUrlencoded(q)
        }

        public fun fragment(): String? =
            fragmentStart?.let { serialization.substring(it + 1) }

        // --- Setters ---

        public fun setFragment(fragment: String?) {
            fragmentStart?.let {
                serialization = serialization.substring(0, it)
            }
            if (fragment != null) {
                fragmentStart = serialization.length
                serialization += "#$fragment"
            } else {
                fragmentStart = null
                stripTrailingSpacesFromOpaquePath()
            }
        }

        public fun setQuery(query: String?) {
            val savedFragment = takeFragment()
            queryStart?.let {
                serialization = serialization.substring(0, it)
                queryStart = null
            }
            if (query != null) {
                queryStart = serialization.length
                serialization += "?$query"
            } else {
                queryStart = null
                if (savedFragment == null) stripTrailingSpacesFromOpaquePath()
            }
            restoreAlreadyParsedFragment(savedFragment)
        }

        public fun queryPairsMut(): UrlQuery {
            val savedFragment = takeFragment()
            val qs: Int
            if (queryStart != null) {
                qs = queryStart!!
            } else {
                qs = serialization.length
                queryStart = qs
                serialization += '?'
            }
            return UrlQuery(this, savedFragment)
        }

        public fun setPath(path: String) {
            val afterPath = takeAfterPath()
            val oldAfterPathPos = serialization.length
            val cbabs = cannotBeABase()
            val pathString =
                if (cbabs) {
                    if (path.startsWith("/")) "%2F${path.substring(1)}" else path
                } else if (path.isEmpty() && !isSpecial() && hasHost()) {
                    ""
                } else {
                    "/${path.trimStart('/')}"
                }
            serialization = serialization.substring(0, pathStart) + pathString
            restoreAfterPath(oldAfterPathPos, afterPath)
        }

        public fun pathSegmentsMut(): Result<PathSegmentsMut> {
            if (cannotBeABase()) return Result.failure(ParseError.NotSupported("cannot be a base"))
            return Result.success(PathSegmentsMut(this))
        }

        public fun setPort(port: Int?): Result<Unit> {
            if (!hasHost() || host() == Host.Domain("") || scheme() == "file") {
                return Result.failure(ParseError.NotSupported("setPort"))
            }
            var effectivePort = port
            if (effectivePort != null && effectivePort == defaultPort(scheme())) {
                effectivePort = null
            }
            setPortInternal(effectivePort)
            return Result.success(Unit)
        }

        public fun setHost(host: String?): Result<Unit> {
            if (cannotBeABase()) return Result.failure(ParseError.CannotSetHost)

            val schemeType = scheme()
            if (host != null) {
                if (host.isEmpty() && isSpecial() && schemeType != "file") {
                    return Result.failure(ParseError.EmptyHost)
                }
                var hostSubstr = host
                if (!host.startsWith("[") || !host.endsWith("]")) {
                    val colonIdx = host.indexOf(':')
                    when {
                        colonIdx == 0 -> return Result.failure(ParseError.InvalidDomainCharacter)
                        colonIdx > 0 -> hostSubstr = host.substring(0, colonIdx)
                    }
                }
                setHostInternal(Host.Domain(hostSubstr), null)
            } else if (hasHost()) {
                if (isSpecial() && schemeType != "file") {
                    return Result.failure(ParseError.EmptyHost)
                }
                if (serialization.length == pathStart) serialization += "/"
                val newPathStart = if (schemeType == "file") schemeEnd + 3 else schemeEnd + 1
                serialization = serialization.substring(0, newPathStart) + serialization.substring(pathStart)
                val offset = pathStart - newPathStart
                pathStart = newPathStart
                usernameEnd = newPathStart
                hostStart = newPathStart
                hostEnd = newPathStart
                this.host = HostInternal.None
                port = null
                queryStart = queryStart?.let { it - offset }
                fragmentStart = fragmentStart?.let { it - offset }
            }
            return Result.success(Unit)
        }

        public fun setIpHost(address: String): Result<Unit> {
            if (cannotBeABase()) return Result.failure(ParseError.NotSupported("setIpHost"))
            val isIpv6 = address.startsWith("[") || (isValidIpv6(address) && address.contains(':'))
            if (isIpv6) {
                val inner = if (address.startsWith("[")) address.substring(1, address.length - 1) else address
                val expanded = expandIpv6(inner)
                val shortened = shortenIpv6(expanded)
                setHostInternal(Host.Domain("[$shortened]"), null)
                this.host = HostInternal.Ipv6(expanded)
            } else {
                setHostInternal(Host.Domain(address), null)
            }
            return Result.success(Unit)
        }

        public fun setPassword(password: String?): Result<Unit> {
            if (!hasHost() || host() == Host.Domain("") || scheme() == "file") {
                return Result.failure(ParseError.NotSupported("setPassword"))
            }
            val pwd = password ?: ""
            if (pwd.isNotEmpty()) {
                val hostAndAfter = serialization.substring(hostStart)
                serialization = serialization.substring(0, usernameEnd) + ":$pwd@$hostAndAfter"
            } else if (usernameEnd < serialization.length && serialization[usernameEnd] == ':') {
                val start = usernameEnd
                val end = if (schemeEnd + 3 == usernameEnd) hostStart else hostStart - 1
                serialization = serialization.substring(0, start) + serialization.substring(end)
                val offset = end - start
                hostStart -= offset
                hostEnd -= offset
                pathStart -= offset
                queryStart = queryStart?.let { it - offset }
                fragmentStart = fragmentStart?.let { it - offset }
            }
            return Result.success(Unit)
        }

        public fun setUsername(username: String): Result<Unit> {
            if (!hasHost() || host() == Host.Domain("") || scheme() == "file") {
                return Result.failure(ParseError.NotSupported("setUsername"))
            }
            val usernameStart = schemeEnd + 3
            val afterUsername = serialization.substring(usernameEnd)
            val oldLen = serialization.length
            serialization = serialization.substring(0, usernameStart) + username
            val newUsernameEmpty = serialization.length == usernameStart
            serialization +=
                when {
                    newUsernameEmpty && afterUsername.startsWith("@") -> afterUsername.substring(1)
                    !newUsernameEmpty && !afterUsername.startsWith("@") && !afterUsername.startsWith(":") -> "@$afterUsername"
                    else -> afterUsername
                }
            val delta = serialization.length - oldLen
            usernameEnd = serialization.length - afterUsername.length +
                if (newUsernameEmpty && afterUsername.startsWith("@")) {
                    1
                } else if (!newUsernameEmpty && !afterUsername.startsWith("@") && !afterUsername.startsWith(":")) {
                    1
                } else {
                    0
                }
            hostStart += delta
            hostEnd += delta
            pathStart += delta
            queryStart = queryStart?.let { it + delta }
            fragmentStart = fragmentStart?.let { it + delta }
            return Result.success(Unit)
        }

        public fun setScheme(scheme: String): Result<Unit> {
            if (scheme.isEmpty() || !scheme.first().isLetter()) {
                return Result.failure(ParseError.NotSupported("setScheme"))
            }
            val newScheme = scheme.lowercase()
            val oldScheme = scheme()
            if (newScheme == "file" || oldScheme == "file") {
                return Result.failure(ParseError.NotSupported("setScheme"))
            }
            val newIsSpecial = newScheme in specialSchemes
            val oldIsSpecial = oldScheme in specialSchemes
            if (newIsSpecial != oldIsSpecial) return Result.failure(ParseError.NotSupported("setScheme"))
            if (newIsSpecial && !hasHost()) return Result.failure(ParseError.NotSupported("setScheme"))
            val oldSchemeEnd = schemeEnd
            schemeEnd = newScheme.length
            serialization = newScheme + serialization.substring(oldSchemeEnd)
            return Result.success(Unit)
        }

        // --- Internal helpers ---

        @PublishedApi internal fun stripTrailingSpacesFromOpaquePath() {
            if (!cannotBeABase()) return
            if (fragmentStart != null) return
            if (queryStart != null) return
            while (serialization.endsWith(" ")) {
                serialization = serialization.substring(0, serialization.length - 1)
            }
        }

        @PublishedApi internal fun takeFragment(): String? {
            val frag =
                fragmentStart?.let {
                    val f = serialization.substring(it + 1)
                    serialization = serialization.substring(0, it)
                    fragmentStart = null
                    f
                }
            return frag
        }

        @PublishedApi internal fun restoreAlreadyParsedFragment(fragment: String?) {
            if (fragment != null) {
                fragmentStart = serialization.length
                serialization += "#$fragment"
            }
        }

        @PublishedApi internal fun takeAfterPath(): String =
            when {
                queryStart != null -> {
                    val after = serialization.substring(queryStart!!)
                    serialization = serialization.substring(0, queryStart!!)
                    after
                }
                fragmentStart != null -> {
                    val after = serialization.substring(fragmentStart!!)
                    serialization = serialization.substring(0, fragmentStart!!)
                    after
                }
                else -> ""
            }

        @PublishedApi internal fun restoreAfterPath(oldAfterPathPosition: Int, afterPath: String) {
            fun adjust(index: Int): Int = index - oldAfterPathPosition + serialization.length
            queryStart = queryStart?.let { adjust(it) }
            fragmentStart = fragmentStart?.let { adjust(it) }
            serialization += afterPath
        }

        @PublishedApi internal fun setPortInternal(newPort: Int?) {
            when {
                port == null && newPort == null -> {}
                port != null && newPort == null -> {
                    serialization = serialization.substring(0, hostEnd) + serialization.substring(pathStart)
                    val offset = pathStart - hostEnd
                    pathStart = hostEnd
                    queryStart = queryStart?.let { it - offset }
                    fragmentStart = fragmentStart?.let { it - offset }
                }
                port != null && port == newPort -> {}
                else -> {
                    val pathAndAfter = serialization.substring(pathStart)
                    serialization = serialization.substring(0, hostEnd) + ":$newPort"
                    val oldPathStart = pathStart
                    pathStart = serialization.length

                    fun adjust(index: Int): Int = index - oldPathStart + pathStart
                    queryStart = queryStart?.let { adjust(it) }
                    fragmentStart = fragmentStart?.let { adjust(it) }
                    serialization += pathAndAfter
                }
            }
            port = newPort
        }

        @PublishedApi internal fun setHostInternal(host: Host<String>, optNewPort: Int?) {
            val oldSuffixPos = if (optNewPort != null) pathStart else hostEnd
            val suffix = serialization.substring(oldSuffixPos)
            serialization = serialization.substring(0, hostStart)
            if (!hasAuthority()) {
                serialization += "//"
                usernameEnd += 2
                hostStart += 2
            }
            serialization +=
                when (host) {
                    is Host.Domain -> host.domain
                    else -> host.toString()
                }
            hostEnd = serialization.length
            this.host = host.toInternal()

            if (optNewPort != null) {
                port = optNewPort
                serialization += ":$optNewPort"
            }
            val newSuffixPos = serialization.length
            serialization += suffix

            fun adjust(index: Int): Int = index - oldSuffixPos + newSuffixPos
            pathStart = adjust(pathStart)
            queryStart = queryStart?.let { adjust(it) }
            fragmentStart = fragmentStart?.let { adjust(it) }
        }

        override fun toString(): String = serialization

        override fun equals(other: Any?): Boolean =
            other is Url && serialization == other.serialization

        override fun hashCode(): Int = serialization.hashCode()
    }

@HiddenFromObjC
public data class UrlQuery(
    internal var url: Url?,
    internal var fragment: String?,
) {
    public fun appendPair(key: String, value: String): UrlQuery {
        val u = url ?: return this
        val q = u.query().orEmpty()
        u.setQuery(
            if (q.isEmpty()) {
                "${encodeFormUrlencoded(key)}=${encodeFormUrlencoded(value)}"
            } else {
                "$q&${encodeFormUrlencoded(key)}=${encodeFormUrlencoded(value)}"
            },
        )
        return this
    }

    public fun extendPairs(pairs: List<Pair<String, String>>): UrlQuery {
        for ((key, value) in pairs) appendPair(key, value)
        return this
    }

    public fun clear(): UrlQuery {
        url?.setQuery("")
        return this
    }

    public fun finish(): Url? {
        val u = url
        if (u != null) {
            u.restoreAlreadyParsedFragment(fragment)
        }
        url = null
        return u
    }
}

// --- Internal constants ---

internal val specialSchemes =
    setOf(
        "ftp",
        "file",
        "http",
        "https",
        "ws",
        "wss",
    )

private fun encodePathSegment(segment: String): String =
    buildString {
        for (c in segment) {
            when (c) {
                ' ', '"', '#', '<', '>', '?', '^', '`', '{', '}', '|', '%', '\\' -> {
                    val bytes = c.toString().encodeToByteArray()
                    for (b in bytes) {
                        append('%')
                        append((b.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0'))
                    }
                }
                in '\u0000'..'\u001F', in '\u007F'..'\u009F' -> {
                    val bytes = c.toString().encodeToByteArray()
                    for (b in bytes) {
                        append('%')
                        append((b.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0'))
                    }
                }
                else -> append(c)
            }
        }
    }

private fun decodePercent(s: String): String? {
    val bytes = mutableListOf<Byte>()
    var i = 0
    while (i < s.length) {
        if (s[i] == '%') {
            if (i + 2 >= s.length) return null
            val hex = s.substring(i + 1, i + 3)
            val byteVal = hex.toIntOrNull(16) ?: return null
            bytes.add(byteVal.toByte())
            i += 3
        } else {
            val charBytes = s[i].toString().encodeToByteArray()
            for (b in charBytes) bytes.add(b)
            i++
        }
    }
    val byteArray = ByteArray(bytes.size) { bytes[it] }
    return try {
        byteArray.decodeToString()
    } catch (_: Exception) {
        null
    }
}

internal fun pathToFileUrl(path: String): Result<Url> {
    val isWindowsDrive =
        path.length >= 2 &&
            path[0].isLetter() &&
            (path[1] == ':' && (path.length == 2 || path[2] == '/' || path[2] == '\\'))
    val isWindowsPrefix = path.startsWith("\\\\?\\") || path.startsWith("//?/")
    val isUnc = (path.startsWith("\\\\") || path.startsWith("//")) && !isWindowsPrefix

    val normalizedPath = path.replace('\\', '/')
    if (!normalizedPath.startsWith("/") && !isWindowsDrive && !isWindowsPrefix && !isUnc) {
        return Result.failure(ParseError.NotSupported("relative path"))
    }

    if (isUnc) {
        val trimmed = normalizedPath.trimStart('/')
        val slashIdx = trimmed.indexOf('/')
        val server = if (slashIdx >= 0) trimmed.substring(0, slashIdx) else trimmed
        val rest = if (slashIdx >= 0) trimmed.substring(slashIdx) else "/"
        val parsedHost = Host.parse(server).getOrNull() ?: return Result.failure(ParseError.InvalidDomainCharacter)
        val hostStr = server
        val rawSegments = rest.split('/').filter { it.isNotEmpty() }
        val encodedSegments = rawSegments.map { encodePathSegment(it) }
        val pathStr = "/" + encodedSegments.joinToString("/")
        val serialization = "file://$hostStr$pathStr"
        val schemeEnd = 4
        val usernameEnd = 7
        val hostStart = 7
        val hostEnd = hostStart + hostStr.length
        val hostInternal = parsedHost.toInternal()
        val pathStart = hostEnd
        return Result.success(
            Url(
                serialization = serialization,
                schemeEnd = schemeEnd,
                usernameEnd = usernameEnd,
                hostStart = hostStart,
                hostEnd = hostEnd,
                host = hostInternal,
                port = null,
                pathStart = pathStart,
                queryStart = null,
                fragmentStart = null,
            ),
        )
    }

    if (isWindowsDrive) {
        val drive = normalizedPath.substring(0, 2)
        val rest = if (normalizedPath.length > 2) normalizedPath.substring(2) else ""
        val rawSegments = rest.split('/').filter { it.isNotEmpty() }
        val encodedSegments = rawSegments.map { encodePathSegment(it) }
        val pathStr = if (encodedSegments.isEmpty()) "/$drive" else "/$drive/" + encodedSegments.joinToString("/")
        val serialization = "file://$pathStr"
        return Url.parse(serialization)
    }

    val rawSegments = normalizedPath.split('/').filter { it.isNotEmpty() }
    val encodedSegments = rawSegments.map { encodePathSegment(it) }
    val pathStr = if (encodedSegments.isEmpty()) "/" else "/" + encodedSegments.joinToString("/")
    val serialization = "file://$pathStr"
    return Url.parse(serialization)
}

internal fun fileUrlToPath(url: Url): Result<String> {
    if (url.scheme() != "file") return Result.failure(ParseError.NotSupported("scheme is not file"))
    val h = url.host()
    val hostOk = h == null || (h is Host.Domain && (h.domain == "localhost" || h.domain == ""))
    val isUnc = h != null && !hostOk

    val segments = url.pathSegments() ?: return Result.failure(ParseError.NotSupported("no path segments"))
    val decodedSegments = mutableListOf<String>()
    for (seg in segments) {
        val dec = decodePercent(seg) ?: return Result.failure(ParseError.NotSupported("invalid percent encoding"))
        if ('\u0000' in dec) return Result.failure(ParseError.NotSupported("contains null byte"))
        decodedSegments.add(dec)
    }

    if (isUnc) {
        val server = (h as? Host.Domain)?.domain ?: return Result.failure(ParseError.NotSupported("invalid unc host"))
        val joined = decodedSegments.joinToString("\\")
        return Result.success("\\\\$server\\$joined")
    }

    if (decodedSegments.isNotEmpty() &&
        decodedSegments[0].length == 2 &&
        decodedSegments[0][0].isLetter() &&
        decodedSegments[0][1] == ':'
    ) {
        val drive = decodedSegments[0]
        val rest = decodedSegments.drop(1)
        return Result.success(if (rest.isEmpty()) "$drive\\" else "$drive\\" + rest.joinToString("\\"))
    }

    return Result.success("/" + decodedSegments.joinToString("/"))
}
