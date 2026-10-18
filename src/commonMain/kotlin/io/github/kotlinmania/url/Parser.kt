// port-lint: source parser.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.url

import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.native.HiddenFromObjC

/**
 * Errors that can occur during parsing.
 */
public sealed class ParseError(
    public val details: String,
) : Exception(details) {
    public data object EmptyHost : ParseError("empty host")

    public data object IdnaError : ParseError("invalid international domain name")

    public data object InvalidPort : ParseError("invalid port number")

    public data object InvalidIpv4Address : ParseError("invalid IPv4 address")

    public data object InvalidIpv6Address : ParseError("invalid IPv6 address")

    public data object InvalidDomainCharacter : ParseError("invalid domain character")

    public data object RelativeUrlWithoutBase : ParseError("relative URL without a base")

    public data object RelativeUrlWithCannotBeABaseBase :
        ParseError("relative URL with a cannot-be-a-base base")

    public data object SetHostOnCannotBeABaseUrl :
        ParseError("a cannot-be-a-base URL doesn’t have a host to set")

    public data object Overflow : ParseError("URLs more than 4 GB are not supported")

    public data object CannotSetHost :
        ParseError("a cannot-be-a-base URL doesn’t have a host to set")

    public data object ParseFailed : ParseError("parse failed")

    public data class NotSupported(
        val operation: String,
    ) : ParseError("not supported: $operation")

    public data class Other(
        override val message: String,
    ) : ParseError(message)

    public fun fmt(): String = details

    public fun errorDescription(): String = details
}

public typealias ParseResult<T> = Result<T>

/**
 * Non-fatal syntax violations that can occur during parsing.
 */
public enum class SyntaxViolation(
    private val text: String,
) {
    Backslash("backslash"),
    C0SpaceIgnored("leading or trailing control or space character are ignored in URLs"),
    EmbeddedCredentials("embedding authentication information (username or password) in an URL is not recommended"),
    ExpectedDoubleSlash("expected //"),
    ExpectedFileDoubleSlash("expected // after file:"),
    FileWithHostAndWindowsDrive("file: with host and Windows drive letter"),
    NonUrlCodePoint("non-URL code point"),
    NullInFragment("NULL characters are ignored in URL fragment identifiers"),
    PercentDecode("expected 2 hex digits after %"),
    TabOrNewlineIgnored("tabs or newlines are ignored in URLs"),
    UnencodedAtSign("unencoded @ sign in username or password"),
    PercentDecodeExpected("Percent-decoded bytes did not trigger a valid character"),
    NonCharacter("Code point is a non-character"),
    InvalidCodePoint("Code point is invalid in URL"),
    ;

    public fun fmt(): String = text

    public fun errorDescription(): String = text

    override fun toString(): String = text
}

public enum class SchemeType {
    File,
    SpecialNotFile,
    NotSpecial,
    ;

    public fun isSpecial(): Boolean = this != NotSpecial

    public fun isFile(): Boolean = this == File

    public companion object {
        public fun from(scheme: String): SchemeType =
            when (scheme.lowercase()) {
                "http", "https", "ws", "wss", "ftp" -> SpecialNotFile
                "file" -> File
                else -> NotSpecial
            }
    }
}

public fun defaultPort(scheme: String): Int? =
    when (scheme.lowercase()) {
        "http", "ws" -> 80
        "https", "wss" -> 443
        "ftp" -> 21
        else -> null
    }

public typealias EncodingOverride = ((String) -> String)?

internal interface Pattern {
    fun splitPrefix(input: Input): Boolean
}

internal class CharPattern(
    val char: Char,
) : Pattern {
    override fun splitPrefix(input: Input): Boolean = input.nextOrNull() == char
}

internal class StringPattern(
    val str: String,
) : Pattern {
    override fun splitPrefix(input: Input): Boolean {
        for (c in str) {
            if (input.nextOrNull() != c) return false
        }
        return true
    }
}

internal class PredicatePattern(
    val predicate: (Char) -> Boolean,
) : Pattern {
    override fun splitPrefix(input: Input): Boolean = input.nextOrNull()?.let(predicate) ?: false
}

internal enum class Context {
    UrlParser,
    Setter,
    PathSegmentSetter,
}

internal class Input internal constructor(
    private val str: String,
    private var index: Int = 0,
) : Iterator<Char> {
    public typealias Item = Char

    public fun sizeHint(): Pair<Int, Int?> = Pair(0, str.length - index)

    public companion object {
        public fun newNoTrim(input: String): Input = Input(input, 0)

        public fun newTrimTabAndNewlines(
            originalInput: String,
            vfn: ((SyntaxViolation) -> Unit)?,
        ): Input {
            var start = 0
            while (start < originalInput.length && asciiTabOrNewLine(originalInput[start])) {
                start++
            }
            var end = originalInput.length
            while (end > start && asciiTabOrNewLine(originalInput[end - 1])) {
                end--
            }
            val trimmed = originalInput.substring(start, end)
            if (vfn != null) {
                if (trimmed.length < originalInput.length) {
                    vfn(SyntaxViolation.C0SpaceIgnored)
                }
                if (trimmed.any { asciiTabOrNewLine(it) }) {
                    vfn(SyntaxViolation.TabOrNewlineIgnored)
                }
            }
            return Input(trimmed, 0)
        }

        public fun newTrimC0ControlAndSpace(
            originalInput: String,
            vfn: ((SyntaxViolation) -> Unit)?,
        ): Input {
            var start = 0
            while (start < originalInput.length && c0ControlOrSpace(originalInput[start])) {
                start++
            }
            var end = originalInput.length
            while (end > start && c0ControlOrSpace(originalInput[end - 1])) {
                end--
            }
            val trimmed = originalInput.substring(start, end)
            if (vfn != null) {
                if (trimmed.length < originalInput.length) {
                    vfn(SyntaxViolation.C0SpaceIgnored)
                }
                if (trimmed.any { asciiTabOrNewLine(it) }) {
                    vfn(SyntaxViolation.TabOrNewlineIgnored)
                }
            }
            return Input(trimmed, 0)
        }
    }

    public fun clone(): Input = Input(str, index)

    public fun isEmpty(): Boolean = clone().nextOrNull() == null

    public fun startsWith(prefix: String): Boolean = splitPrefix(prefix) != null

    public fun startsWith(predicate: (Char) -> Boolean): Boolean {
        val c = clone().nextOrNull() ?: return false
        return predicate(c)
    }

    public fun splitPrefix(prefix: String): Input? {
        val remaining = clone()
        for (c in prefix) {
            if (remaining.nextOrNull() != c) {
                return null
            }
        }
        return remaining
    }

    public fun splitPrefix(prefix: Char): Input? {
        val remaining = clone()
        if (remaining.nextOrNull() == prefix) {
            return remaining
        }
        return null
    }

    public fun splitFirst(): Pair<Char?, Input> {
        val remaining = clone()
        val c = remaining.nextOrNull()
        return Pair(c, remaining)
    }

    public fun countMatching(predicate: (Char) -> Boolean): Pair<Int, Input> {
        var count = 0
        var current = clone()
        while (true) {
            val nextState = current.clone()
            val c = nextState.nextOrNull()
            if (c != null && predicate(c)) {
                current = nextState
                count++
            } else {
                return Pair(count, current)
            }
        }
    }

    public fun nextUtf8(): Pair<Char, String>? {
        while (index < str.length) {
            val c = str[index]
            index++
            if (!asciiTabOrNewLine(c)) {
                return Pair(c, c.toString())
            }
        }
        return null
    }

    public fun nextOrNull(): Char? {
        while (index < str.length) {
            val c = str[index]
            index++
            if (!asciiTabOrNewLine(c)) {
                return c
            }
        }
        return null
    }

    override fun hasNext(): Boolean {
        var i = index
        while (i < str.length) {
            if (!asciiTabOrNewLine(str[i])) return true
            i++
        }
        return false
    }

    override fun next(): Char =
        nextOrNull() ?: throw NoSuchElementException("No more characters in Input")

    internal fun asRawSlice(): String = if (index < str.length) str.substring(index) else ""
}

internal data class HostAndPort(
    val hostEnd: Int,
    val host: HostInternal,
    val port: Int?,
    val remaining: Input,
)

@HiddenFromObjC
internal class Parser internal constructor(
    internal var serialization: StringBuilder = StringBuilder(),
    internal val baseUrl: Url? = null,
    internal val queryEncodingOverride: EncodingOverride = null,
    internal val violationFn: ((SyntaxViolation) -> Unit)? = null,
    internal val context: Context = Context.UrlParser,
) {
    public companion object {
        public fun forSetter(
            serialization: String,
            context: Context,
            override: EncodingOverride = null,
            violationFn: ((SyntaxViolation) -> Unit)? = null,
        ): Parser =
            Parser(
                serialization = StringBuilder(serialization),
                baseUrl = null,
                queryEncodingOverride = override,
                violationFn = violationFn,
                context = context,
            )

        public fun parsePort(
            input: Input,
            defaultPortFn: () -> Int?,
            context: Context,
        ): ParseResult<Pair<Int?, Input>> {
            var currentInput = input
            var port: Long = 0
            var hasAnyDigit = false
            while (true) {
                val (c, remaining) = currentInput.splitFirst()
                if (c != null && c.isDigit()) {
                    port = port * 10 + (c - '0')
                    if (port > 65535) {
                        return Result.failure(ParseError.InvalidPort)
                    }
                    hasAnyDigit = true
                    currentInput = remaining
                } else if (context == Context.UrlParser && c != null && c !in charArrayOf('/', '\\', '?', '#')) {
                    return Result.failure(ParseError.InvalidPort)
                } else {
                    break
                }
            }

            if (!hasAnyDigit && context == Context.Setter && !currentInput.isEmpty()) {
                return Result.failure(ParseError.InvalidPort)
            }

            var optPort: Int? = if (hasAnyDigit) port.toInt() else null
            if (!hasAnyDigit || optPort == defaultPortFn()) {
                optPort = null
            }
            return Result.success(Pair(optPort, currentInput))
        }

        public fun parseHost(
            input: Input,
            schemeType: SchemeType,
        ): ParseResult<Pair<Host<String>, Input>> {
            if (schemeType.isFile()) {
                return getFileHost(input)
            }
            val rawSlice = input.asRawSlice()
            var insideSquareBrackets = false
            var hasIgnoredChars = false
            var nonIgnoredChars = 0
            var bytes = 0
            for (c in rawSlice) {
                if (c == ':' && !insideSquareBrackets) break
                if (c == '\\' && schemeType.isSpecial()) break
                if (c == '/' || c == '?' || c == '#') break
                if (asciiTabOrNewLine(c)) {
                    hasIgnoredChars = true
                } else if (c == '[') {
                    insideSquareBrackets = true
                    nonIgnoredChars++
                } else if (c == ']') {
                    insideSquareBrackets = false
                    nonIgnoredChars++
                } else {
                    nonIgnoredChars++
                }
                bytes += c.toString().encodeToByteArray().size
            }
            val hostStr =
                if (hasIgnoredChars) {
                    val sb = StringBuilder()
                    repeat(nonIgnoredChars) {
                        val c = input.nextOrNull() ?: return@repeat
                        sb.append(c)
                    }
                    sb.toString()
                } else {
                    repeat(nonIgnoredChars) {
                        input.nextOrNull()
                    }
                    rawSlice.substring(0, minOf(rawSlice.length, nonIgnoredChars))
                }
            if (schemeType == SchemeType.SpecialNotFile && hostStr.isEmpty()) {
                return Result.failure(ParseError.EmptyHost)
            }
            if (!schemeType.isSpecial()) {
                val hostRes = Host.parseOpaque(hostStr)
                if (hostRes.isFailure) {
                    return Result.failure(
                        hostRes.exceptionOrNull() as? ParseError ?: ParseError.InvalidDomainCharacter,
                    )
                }
                return Result.success(Pair(hostRes.getOrThrow(), input))
            }
            val hostResult = Host.parse(hostStr)
            if (hostResult.isFailure) {
                return Result.failure(
                    hostResult.exceptionOrNull() as? ParseError ?: ParseError.InvalidDomainCharacter,
                )
            }
            return Result.success(Pair(hostResult.getOrThrow(), input))
        }

        public fun getFileHost(input: Input): ParseResult<Pair<Host<String>, Input>> {
            val fileHostRes = fileHost(input)
            if (fileHostRes.isFailure) {
                return Result.failure(
                    fileHostRes.exceptionOrNull() as? ParseError ?: ParseError.EmptyHost,
                )
            }
            val (_, hostStr, remaining) = fileHostRes.getOrThrow()
            val parsed = Host.parse(hostStr)
            if (parsed.isFailure) {
                return Result.failure(
                    parsed.exceptionOrNull() as? ParseError ?: ParseError.InvalidDomainCharacter,
                )
            }
            val host =
                when (val h = parsed.getOrThrow()) {
                    is Host.Domain -> if (h.domain == "localhost") Host.Domain("") else h
                    is Host.Ipv4 -> h
                    is Host.Ipv6 -> h
                }
            return Result.success(Pair(host, remaining))
        }

        public fun fileHost(input: Input): ParseResult<Triple<Boolean, String, Input>> {
            val rawSlice = input.asRawSlice()
            var hasIgnoredChars = false
            var nonIgnoredChars = 0
            for (c in rawSlice) {
                if (c == '/' || c == '\\' || c == '?' || c == '#') break
                if (asciiTabOrNewLine(c)) {
                    hasIgnoredChars = true
                } else {
                    nonIgnoredChars++
                }
            }
            val remaining = input.clone()
            val hostStr =
                if (hasIgnoredChars) {
                    val sb = StringBuilder()
                    repeat(nonIgnoredChars) {
                        val c = remaining.nextOrNull() ?: return@repeat
                        sb.append(c)
                    }
                    sb.toString()
                } else {
                    repeat(nonIgnoredChars) {
                        remaining.nextOrNull()
                    }
                    rawSlice.substring(0, minOf(rawSlice.length, nonIgnoredChars))
                }
            if (isWindowsDriveLetter(hostStr)) {
                return Result.success(Triple(false, "", input))
            }
            return Result.success(Triple(true, hostStr, remaining))
        }

        public fun lastSlashCanBeRemoved(
            serialization: String,
            pathStart: Int,
        ): Boolean {
            val urlBeforeSegment = serialization.substring(0, serialization.length - 1)
            val segmentBeforeStart = urlBeforeSegment.lastIndexOf('/')
            return if (segmentBeforeStart >= 0) {
                segmentBeforeStart >= pathStart &&
                    !pathStartsWithWindowsDriveLetter(serialization.substring(segmentBeforeStart))
            } else {
                false
            }
        }
    }

    public fun parseUrl(input: String): ParseResult<Url> {
        val (firstChar, _) = Input.newNoTrim(input).splitFirst()
        if (firstChar != null && c0ControlOrSpace(firstChar)) {
            logViolation(SyntaxViolation.C0SpaceIgnored)
        }
        val trimmedInput = Input.newTrimC0ControlAndSpace(input, violationFn)
        val schemeRes = parseScheme(trimmedInput.clone())
        if (schemeRes.isSuccess) {
            val (schemeType, schemeEnd, remaining) = schemeRes.getOrThrow()
            return parseWithScheme(remaining, schemeType, schemeEnd)
        }

        // No-scheme state
        if (baseUrl != null) {
            if (trimmedInput.startsWith("#")) {
                return fragmentOnly(baseUrl, trimmedInput)
            } else if (baseUrl.cannotBeABase()) {
                return Result.failure(ParseError.RelativeUrlWithCannotBeABaseBase)
            } else {
                val schemeType = SchemeType.from(baseUrl.scheme())
                serialization.clear()
                return if (schemeType.isFile()) {
                    parseFile(trimmedInput, schemeType, baseUrl)
                } else {
                    parseRelative(trimmedInput, schemeType, baseUrl)
                }
            }
        } else {
            return Result.failure(ParseError.RelativeUrlWithoutBase)
        }
    }

    public fun parseScheme(input: Input): ParseResult<Triple<SchemeType, Int, Input>> {
        val remaining = input.clone()
        val (firstChar, _) = remaining.splitFirst()
        if (firstChar == null || !asciiAlpha(firstChar)) {
            return Result.failure(ParseError.RelativeUrlWithoutBase)
        }
        serialization.clear()
        while (remaining.hasNext()) {
            val c = remaining.next()
            when (c) {
                in 'a'..'z', in '0'..'9', '+', '-', '.' -> {
                    serialization.append(c)
                }

                in 'A'..'Z' -> {
                    serialization.append(c.lowercaseChar())
                }

                ':' -> {
                    val schemeEnd = serialization.length
                    val schemeType = SchemeType.from(serialization.toString())
                    return Result.success(Triple(schemeType, schemeEnd, remaining))
                }

                else -> {
                    serialization.clear()
                    return Result.failure(ParseError.RelativeUrlWithoutBase)
                }
            }
        }
        if (context == Context.Setter) {
            val schemeEnd = serialization.length
            val schemeType = SchemeType.from(serialization.toString())
            return Result.success(Triple(schemeType, schemeEnd, remaining))
        } else {
            serialization.clear()
            return Result.failure(ParseError.RelativeUrlWithoutBase)
        }
    }

    public fun parseWithScheme(
        input: Input,
        schemeType: SchemeType,
        schemeEnd: Int,
    ): ParseResult<Url> {
        serialization.append(':')
        return when (schemeType) {
            SchemeType.File -> {
                logViolationIf(SyntaxViolation.ExpectedFileDoubleSlash) { !input.startsWith("//") }
                val baseFileUrl =
                    if (baseUrl != null && baseUrl.scheme() == "file") {
                        baseUrl
                    } else {
                        null
                    }
                serialization.clear()
                parseFile(input, schemeType, baseFileUrl)
            }

            SchemeType.SpecialNotFile -> {
                val (slashesCount, remaining) = input.countMatching { it == '/' || it == '\\' }
                if (baseUrl != null) {
                    if (slashesCount < 2 && baseUrl.scheme() == serialization.substring(0, schemeEnd)) {
                        serialization.clear()
                        return parseRelative(input, schemeType, baseUrl)
                    }
                }
                logViolationIf(SyntaxViolation.ExpectedDoubleSlash) {
                    val slashStr =
                        buildString {
                            val iter = input.clone()
                            while (iter.hasNext()) {
                                val c = iter.next()
                                if (c == '/' || c == '\\') append(c) else break
                            }
                        }
                    slashStr != "//"
                }
                afterDoubleSlash(remaining, schemeType, schemeEnd)
            }

            SchemeType.NotSpecial -> {
                parseNonSpecial(input, schemeType, schemeEnd)
            }
        }
    }

    public fun parseNonSpecial(
        input: Input,
        schemeType: SchemeType,
        schemeEnd: Int,
    ): ParseResult<Url> {
        val afterSlashes = input.splitPrefix("//")
        if (afterSlashes != null) {
            return afterDoubleSlash(afterSlashes, schemeType, schemeEnd)
        }
        val pathStart = toU32(serialization.length).getOrThrow()
        val usernameEnd = pathStart
        val hostStart = pathStart
        val hostEnd = pathStart
        val host = HostInternal.None
        val port: Int? = null
        val afterSlash = input.splitPrefix('/')
        val remaining =
            if (afterSlash != null) {
                serialization.append('/')
                parsePath(schemeType, booleanArrayOf(false), pathStart, afterSlash)
            } else {
                parseCannotBeABasePath(input)
            }
        return withQueryAndFragment(
            schemeType,
            schemeEnd,
            usernameEnd,
            hostStart,
            hostEnd,
            host,
            port,
            pathStart,
            remaining,
        )
    }

    public fun parseFile(
        input: Input,
        schemeType: SchemeType,
        baseFileUrl: Url?,
    ): ParseResult<Url> {
        val (firstChar, inputAfterFirstChar) = input.splitFirst()
        if (firstChar == '/' || firstChar == '\\') {
            logViolationIf(SyntaxViolation.Backslash) { firstChar == '\\' }
            val (nextChar, inputAfterNextChar) = inputAfterFirstChar.splitFirst()
            if (nextChar == '/' || nextChar == '\\') {
                logViolationIf(SyntaxViolation.Backslash) { nextChar == '\\' }
                serialization.append("file://")
                val schemeEnd = "file".length
                val hostStart = "file://".length
                val parseFileHostRes = parseFileHost(inputAfterNextChar)
                if (parseFileHostRes.isFailure) {
                    return Result.failure(parseFileHostRes.exceptionOrNull() ?: ParseError.ParseFailed)
                }
                val (pathStartFlag, hostRaw, remainingHost) = parseFileHostRes.getOrThrow()
                var host = hostRaw
                var hostEnd = toU32(serialization.length).getOrThrow()
                val hasHostArray = booleanArrayOf(host != HostInternal.None)
                val remaining =
                    if (pathStartFlag) {
                        parsePathStart(SchemeType.File, hasHostArray, remainingHost)
                    } else {
                        val pathStart = serialization.length
                        serialization.append('/')
                        parsePath(SchemeType.File, hasHostArray, pathStart, remainingHost)
                    }

                if (!hasHostArray[0]) {
                    serialization.deleteRange(hostStart, hostEnd)
                    hostEnd = hostStart
                    host = HostInternal.None
                }
                val (queryStart, fragmentStart) =
                    parseQueryAndFragment(schemeType, schemeEnd, remaining).getOrThrow()
                return Result.success(
                    Url(
                        serialization = serialization.toString(),
                        schemeEnd = schemeEnd,
                        usernameEnd = hostStart,
                        hostStart = hostStart,
                        hostEnd = hostEnd,
                        host = host,
                        port = null,
                        pathStart = hostEnd,
                        queryStart = queryStart,
                        fragmentStart = fragmentStart,
                    ),
                )
            } else {
                serialization.append("file://")
                val schemeEnd = "file".length
                val hostStart = "file://".length
                var hostEnd = hostStart
                var host: HostInternal = HostInternal.None
                if (!startsWithWindowsDriveLetterSegment(inputAfterFirstChar)) {
                    if (baseFileUrl != null) {
                        val firstSegment = baseFileUrl.pathSegments()?.firstOrNull()
                        if (firstSegment != null && isNormalizedWindowsDriveLetter(firstSegment)) {
                            serialization.append('/')
                            serialization.append(firstSegment)
                        } else {
                            val hostStr = baseFileUrl.hostStr()
                            if (hostStr != null) {
                                serialization.append(hostStr)
                                hostEnd = serialization.length
                                host = baseFileUrl.host
                            }
                        }
                    }
                }

                val parsePathInput = input

                val remaining =
                    parsePath(SchemeType.File, booleanArrayOf(false), hostEnd, parsePathInput)

                val (queryStart, fragmentStart) =
                    parseQueryAndFragment(schemeType, schemeEnd, remaining).getOrThrow()

                return Result.success(
                    Url(
                        serialization = serialization.toString(),
                        schemeEnd = schemeEnd,
                        usernameEnd = hostStart,
                        hostStart = hostStart,
                        hostEnd = hostEnd,
                        host = host,
                        port = null,
                        pathStart = hostEnd,
                        queryStart = queryStart,
                        fragmentStart = fragmentStart,
                    ),
                )
            }
        }

        if (baseFileUrl != null) {
            when (firstChar) {
                null -> {
                    val beforeFragment =
                        if (baseFileUrl.fragmentStart != null) {
                            baseFileUrl.serialization.substring(0, baseFileUrl.fragmentStart!!)
                        } else {
                            baseFileUrl.serialization
                        }
                    serialization.append(beforeFragment)
                    return Result.success(
                        Url(
                            serialization = serialization.toString(),
                            schemeEnd = baseFileUrl.schemeEnd,
                            usernameEnd = baseFileUrl.usernameEnd,
                            hostStart = baseFileUrl.hostStart,
                            hostEnd = baseFileUrl.hostEnd,
                            host = baseFileUrl.host,
                            port = baseFileUrl.port,
                            pathStart = baseFileUrl.pathStart,
                            queryStart = baseFileUrl.queryStart,
                            fragmentStart = null,
                        ),
                    )
                }

                '?' -> {
                    val beforeQuery =
                        when {
                            baseFileUrl.queryStart != null -> {
                                baseFileUrl.serialization.substring(0, baseFileUrl.queryStart!!)
                            }

                            baseFileUrl.fragmentStart != null -> {
                                baseFileUrl.serialization.substring(0, baseFileUrl.fragmentStart!!)
                            }

                            else -> {
                                baseFileUrl.serialization
                            }
                        }
                    serialization.append(beforeQuery)
                    val (queryStart, fragmentStart) =
                        parseQueryAndFragment(schemeType, baseFileUrl.schemeEnd, input).getOrThrow()
                    return Result.success(
                        Url(
                            serialization = serialization.toString(),
                            schemeEnd = baseFileUrl.schemeEnd,
                            usernameEnd = baseFileUrl.usernameEnd,
                            hostStart = baseFileUrl.hostStart,
                            hostEnd = baseFileUrl.hostEnd,
                            host = baseFileUrl.host,
                            port = baseFileUrl.port,
                            pathStart = baseFileUrl.pathStart,
                            queryStart = queryStart,
                            fragmentStart = fragmentStart,
                        ),
                    )
                }

                '#' -> {
                    return fragmentOnly(baseFileUrl, input)
                }

                else -> {
                    if (!startsWithWindowsDriveLetterSegment(input)) {
                        val beforeQuery =
                            when {
                                baseFileUrl.queryStart != null -> {
                                    baseFileUrl.serialization.substring(0, baseFileUrl.queryStart!!)
                                }

                                baseFileUrl.fragmentStart != null -> {
                                    baseFileUrl.serialization.substring(0, baseFileUrl.fragmentStart!!)
                                }

                                else -> {
                                    baseFileUrl.serialization
                                }
                            }
                        serialization.append(beforeQuery)
                        shortenPath(SchemeType.File, baseFileUrl.pathStart)
                        val remaining =
                            parsePath(SchemeType.File, booleanArrayOf(true), baseFileUrl.pathStart, input)
                        return withQueryAndFragment(
                            SchemeType.File,
                            baseFileUrl.schemeEnd,
                            baseFileUrl.usernameEnd,
                            baseFileUrl.hostStart,
                            baseFileUrl.hostEnd,
                            baseFileUrl.host,
                            baseFileUrl.port,
                            baseFileUrl.pathStart,
                            remaining,
                        )
                    } else {
                        serialization.append("file:///")
                        val schemeEnd = "file".length
                        val pathStart = "file://".length
                        val remaining =
                            parsePath(SchemeType.File, booleanArrayOf(false), pathStart, input)
                        val (queryStart, fragmentStart) =
                            parseQueryAndFragment(SchemeType.File, schemeEnd, remaining).getOrThrow()
                        return Result.success(
                            Url(
                                serialization = serialization.toString(),
                                schemeEnd = schemeEnd,
                                usernameEnd = pathStart,
                                hostStart = pathStart,
                                hostEnd = pathStart,
                                host = HostInternal.None,
                                port = null,
                                pathStart = pathStart,
                                queryStart = queryStart,
                                fragmentStart = fragmentStart,
                            ),
                        )
                    }
                }
            }
        } else {
            serialization.append("file:///")
            val schemeEnd = "file".length
            val pathStart = "file://".length
            val remaining = parsePath(SchemeType.File, booleanArrayOf(false), pathStart, input)
            val (queryStart, fragmentStart) =
                parseQueryAndFragment(SchemeType.File, schemeEnd, remaining).getOrThrow()
            return Result.success(
                Url(
                    serialization = serialization.toString(),
                    schemeEnd = schemeEnd,
                    usernameEnd = pathStart,
                    hostStart = pathStart,
                    hostEnd = pathStart,
                    host = HostInternal.None,
                    port = null,
                    pathStart = pathStart,
                    queryStart = queryStart,
                    fragmentStart = fragmentStart,
                ),
            )
        }
    }

    public fun parseRelative(
        input: Input,
        schemeType: SchemeType,
        baseUrl: Url,
    ): ParseResult<Url> {
        val (firstChar, inputAfterFirstChar) = input.splitFirst()
        when (firstChar) {
            null -> {
                val beforeFragment =
                    if (baseUrl.fragmentStart != null) {
                        baseUrl.serialization.substring(0, baseUrl.fragmentStart!!)
                    } else {
                        baseUrl.serialization
                    }
                serialization.append(beforeFragment)
                return Result.success(
                    Url(
                        serialization = serialization.toString(),
                        schemeEnd = baseUrl.schemeEnd,
                        usernameEnd = baseUrl.usernameEnd,
                        hostStart = baseUrl.hostStart,
                        hostEnd = baseUrl.hostEnd,
                        host = baseUrl.host,
                        port = baseUrl.port,
                        pathStart = baseUrl.pathStart,
                        queryStart = baseUrl.queryStart,
                        fragmentStart = null,
                    ),
                )
            }

            '?' -> {
                val beforeQuery =
                    when {
                        baseUrl.queryStart != null -> {
                            baseUrl.serialization.substring(0, baseUrl.queryStart!!)
                        }

                        baseUrl.fragmentStart != null -> {
                            baseUrl.serialization.substring(0, baseUrl.fragmentStart!!)
                        }

                        else -> {
                            baseUrl.serialization
                        }
                    }
                serialization.append(beforeQuery)
                val (queryStart, fragmentStart) =
                    parseQueryAndFragment(schemeType, baseUrl.schemeEnd, input).getOrThrow()
                return Result.success(
                    Url(
                        serialization = serialization.toString(),
                        schemeEnd = baseUrl.schemeEnd,
                        usernameEnd = baseUrl.usernameEnd,
                        hostStart = baseUrl.hostStart,
                        hostEnd = baseUrl.hostEnd,
                        host = baseUrl.host,
                        port = baseUrl.port,
                        pathStart = baseUrl.pathStart,
                        queryStart = queryStart,
                        fragmentStart = fragmentStart,
                    ),
                )
            }

            '#' -> {
                return fragmentOnly(baseUrl, input)
            }

            '/', '\\' -> {
                val (slashesCount, remaining) = input.countMatching { it == '/' || it == '\\' }
                if (slashesCount >= 2) {
                    logViolationIf(SyntaxViolation.ExpectedDoubleSlash) {
                        val slashStr =
                            buildString {
                                val iter = input.clone()
                                while (iter.hasNext()) {
                                    val c = iter.next()
                                    if (c == '/' || c == '\\') append(c) else break
                                }
                            }
                        slashStr != "//"
                    }
                    val schemeEnd = baseUrl.schemeEnd
                    serialization.append(baseUrl.serialization.substring(0, schemeEnd + 1))
                    val afterPrefix = input.splitPrefix("//")
                    return if (afterPrefix != null) {
                        afterDoubleSlash(afterPrefix, schemeType, schemeEnd)
                    } else {
                        afterDoubleSlash(remaining, schemeType, schemeEnd)
                    }
                }
                val pathStart = baseUrl.pathStart
                serialization.append(baseUrl.serialization.substring(0, pathStart))
                serialization.append('/')
                val remainingPath =
                    parsePath(schemeType, booleanArrayOf(true), pathStart, inputAfterFirstChar)
                return withQueryAndFragment(
                    schemeType,
                    baseUrl.schemeEnd,
                    baseUrl.usernameEnd,
                    baseUrl.hostStart,
                    baseUrl.hostEnd,
                    baseUrl.host,
                    baseUrl.port,
                    baseUrl.pathStart,
                    remainingPath,
                )
            }

            else -> {
                val beforeQuery =
                    when {
                        baseUrl.queryStart != null -> {
                            baseUrl.serialization.substring(0, baseUrl.queryStart!!)
                        }

                        baseUrl.fragmentStart != null -> {
                            baseUrl.serialization.substring(0, baseUrl.fragmentStart!!)
                        }

                        else -> {
                            baseUrl.serialization
                        }
                    }
                serialization.append(beforeQuery)
                popPath(schemeType, baseUrl.pathStart)
                if (serialization.length == baseUrl.pathStart && (SchemeType.from(baseUrl.scheme()).isSpecial() || !input.isEmpty())) {
                    serialization.append('/')
                }
                val (fc, remainingInput) = input.splitFirst()
                val remainingPath =
                    if (fc == '/') {
                        parsePath(schemeType, booleanArrayOf(true), baseUrl.pathStart, remainingInput)
                    } else {
                        parsePath(schemeType, booleanArrayOf(true), baseUrl.pathStart, input)
                    }
                return withQueryAndFragment(
                    schemeType,
                    baseUrl.schemeEnd,
                    baseUrl.usernameEnd,
                    baseUrl.hostStart,
                    baseUrl.hostEnd,
                    baseUrl.host,
                    baseUrl.port,
                    baseUrl.pathStart,
                    remainingPath,
                )
            }
        }
    }

    public fun afterDoubleSlash(
        input: Input,
        schemeType: SchemeType,
        schemeEnd: Int,
    ): ParseResult<Url> {
        serialization.append('/')
        serialization.append('/')
        val beforeAuthority = serialization.length
        val userinfoRes = parseUserinfo(input, schemeType)
        if (userinfoRes.isFailure) {
            return Result.failure(userinfoRes.exceptionOrNull() ?: ParseError.ParseFailed)
        }
        val (usernameEnd, remainingAfterUserinfo) = userinfoRes.getOrThrow()
        val hasAuthority = beforeAuthority != serialization.length
        val hostStart = toU32(serialization.length).getOrThrow()
        val hostAndPortRes = parseHostAndPort(remainingAfterUserinfo, schemeEnd, schemeType)
        if (hostAndPortRes.isFailure) {
            return Result.failure(hostAndPortRes.exceptionOrNull() ?: ParseError.ParseFailed)
        }
        val hostAndPort = hostAndPortRes.getOrThrow()
        val hostEnd = hostAndPort.hostEnd
        val host = hostAndPort.host
        val port = hostAndPort.port
        val remainingAfterHost = hostAndPort.remaining
        if (host == HostInternal.None && hasAuthority) {
            return Result.failure(ParseError.EmptyHost)
        }
        val pathStart = toU32(serialization.length).getOrThrow()
        val remaining =
            parsePathStart(schemeType, booleanArrayOf(true), remainingAfterHost)
        return withQueryAndFragment(
            schemeType,
            schemeEnd,
            usernameEnd,
            hostStart,
            hostEnd,
            host,
            port,
            pathStart,
            remaining,
        )
    }

    public fun parseUserinfo(
        input: Input,
        schemeType: SchemeType,
    ): ParseResult<Pair<Int, Input>> {
        var lastAt: Pair<Int, Input>? = null
        val remaining = input.clone()
        var charCount = 0
        while (remaining.hasNext()) {
            val c = remaining.next()
            when (c) {
                '@' -> {
                    if (lastAt != null) {
                        logViolation(SyntaxViolation.UnencodedAtSign)
                    } else {
                        logViolation(SyntaxViolation.EmbeddedCredentials)
                    }
                    lastAt = Pair(charCount, remaining.clone())
                }

                '/', '?', '#' -> {
                    break
                }

                '\\' -> {
                    if (schemeType.isSpecial()) break
                }

                else -> {}
            }
            charCount++
        }

        val (userinfoCharCountVal, remainingAfterAt) =
            when {
                lastAt == null -> {
                    return Result.success(Pair(toU32(serialization.length).getOrThrow(), input))
                }

                lastAt.first == 0 -> {
                    val (c, _) = lastAt.second.splitFirst()
                    if (c == '/' || c == '?' || c == '#' || (schemeType.isSpecial() && c == '\\')) {
                        return Result.failure(ParseError.EmptyHost)
                    }
                    return Result.success(Pair(toU32(serialization.length).getOrThrow(), lastAt.second))
                }

                else -> {
                    lastAt
                }
            }

        var userinfoCharCount = userinfoCharCountVal
        var usernameEnd: Int? = null
        var hasPassword = false
        var hasUsername = false
        val currentInput = input.clone()
        while (userinfoCharCount > 0) {
            val pair = currentInput.nextUtf8() ?: break
            val c = pair.first
            val utf8C = pair.second
            userinfoCharCount--
            if (c == ':' && usernameEnd == null) {
                usernameEnd = toU32(serialization.length).getOrThrow()
                if (userinfoCharCount > 0) {
                    serialization.append(':')
                    hasPassword = true
                }
            } else {
                if (!hasPassword) {
                    hasUsername = true
                }
                checkUrlCodePoint(c, currentInput)
                serialization.append(PercentEncoding.utf8PercentEncode(utf8C, PercentEncoding::shouldEncodeUserInfo))
            }
        }
        val finalUsernameEnd = usernameEnd ?: toU32(serialization.length).getOrThrow()
        if (hasUsername || hasPassword) {
            serialization.append('@')
        }
        return Result.success(Pair(finalUsernameEnd, remainingAfterAt))
    }

    internal fun parseHostAndPort(
        input: Input,
        schemeEnd: Int,
        schemeType: SchemeType,
    ): ParseResult<HostAndPort> {
        val hostRes = parseHost(input, schemeType)
        if (hostRes.isFailure) {
            return Result.failure(hostRes.exceptionOrNull() ?: ParseError.ParseFailed)
        }
        val (host, remainingAfterHost) = hostRes.getOrThrow()
        serialization.append(host.toString())
        val hostEnd = toU32(serialization.length).getOrThrow()
        if (host is Host.Domain && host.domain.isEmpty()) {
            if (remainingAfterHost.startsWith(":")) {
                return Result.failure(ParseError.EmptyHost)
            }
            if (schemeType.isSpecial()) {
                return Result.failure(ParseError.EmptyHost)
            }
        }

        val afterColon = remainingAfterHost.splitPrefix(':')
        val (port, remaining) =
            if (afterColon != null) {
                val schemeDefaultPort = { defaultPort(serialization.substring(0, schemeEnd)) }
                val portRes = parsePort(afterColon, schemeDefaultPort, context)
                if (portRes.isFailure) {
                    return Result.failure(portRes.exceptionOrNull() ?: ParseError.InvalidPort)
                }
                val (parsedPort, remainingAfterPort) = portRes.getOrThrow()
                if (parsedPort != null) {
                    serialization.append(':')
                    serialization.append(fastU16ToStr(parsedPort))
                }
                Pair(parsedPort, remainingAfterPort)
            } else {
                Pair(null, remainingAfterHost)
            }
        return Result.success(HostAndPort(hostEnd, host.toInternal(), port, remaining))
    }

    internal fun parseFileHost(input: Input): ParseResult<Triple<Boolean, HostInternal, Input>> {
        val fileHostRes = fileHost(input)
        if (fileHostRes.isFailure) {
            return Result.failure(fileHostRes.exceptionOrNull() ?: ParseError.ParseFailed)
        }
        val (_, hostStr, remaining) = fileHostRes.getOrThrow()
        val hasHost: Boolean
        val host: HostInternal
        if (hostStr.isEmpty()) {
            hasHost = false
            host = HostInternal.None
        } else {
            val parsedRes = Host.parse(hostStr)
            if (parsedRes.isFailure) {
                return Result.failure(parsedRes.exceptionOrNull() ?: ParseError.InvalidDomainCharacter)
            }
            val parsed = parsedRes.getOrThrow()
            if (parsed is Host.Domain && parsed.domain == "localhost") {
                hasHost = false
                host = HostInternal.None
            } else {
                serialization.append(parsed.toString())
                hasHost = true
                host = parsed.toInternal()
            }
        }
        return Result.success(Triple(hasHost, host, remaining))
    }

    public fun parsePathStart(
        schemeType: SchemeType,
        hasHost: BooleanArray,
        input: Input,
    ): Input {
        val pathStart = serialization.length
        val (maybeC, remaining) = input.splitFirst()
        if (schemeType.isSpecial()) {
            if (maybeC == '\\') {
                logViolation(SyntaxViolation.Backslash)
            }
            if (!serialization.endsWith('/')) {
                serialization.append('/')
                if (maybeC == '/' || maybeC == '\\') {
                    return parsePath(schemeType, hasHost, pathStart, remaining)
                }
            }
            return parsePath(schemeType, hasHost, pathStart, input)
        } else if (maybeC == '?' || maybeC == '#') {
            return input
        }

        if (maybeC != null && maybeC != '/') {
            serialization.append('/')
        }
        return parsePath(schemeType, hasHost, pathStart, input)
    }

    public fun parsePath(
        schemeType: SchemeType,
        hasHost: BooleanArray,
        pathStart: Int,
        input: Input,
    ): Input {
        fun pushPending(text: String, ctx: Context) {
            if (text.isEmpty()) return
            if (ctx == Context.PathSegmentSetter) {
                if (schemeType.isSpecial()) {
                    serialization.append(PercentEncoding.utf8PercentEncode(text, PercentEncoding::shouldEncodeSpecialPathSegment))
                } else {
                    serialization.append(PercentEncoding.utf8PercentEncode(text, PercentEncoding::shouldEncodePathSegment))
                }
            } else {
                serialization.append(PercentEncoding.utf8PercentEncode(text, PercentEncoding::shouldEncodePath))
            }
        }

        var currentInput = input
        while (true) {
            var segmentStart = serialization.length
            var endsWithSlash = false
            val buffer = StringBuilder()

            while (true) {
                val inputBeforeC = currentInput.clone()
                val (c, nextInput) = currentInput.splitFirst()
                if (c == null) {
                    pushPending(buffer.toString(), context)
                    break
                }
                currentInput = nextInput
                when {
                    asciiTabOrNewLine(c) -> {
                        pushPending(buffer.toString(), context)
                        buffer.clear()
                    }

                    c == '/' && context != Context.PathSegmentSetter -> {
                        pushPending(buffer.toString(), context)
                        serialization.append(c)
                        endsWithSlash = true
                        break
                    }

                    c == '\\' && context != Context.PathSegmentSetter && schemeType.isSpecial() -> {
                        pushPending(buffer.toString(), context)
                        logViolation(SyntaxViolation.Backslash)
                        serialization.append('/')
                        endsWithSlash = true
                        break
                    }

                    (c == '?' || c == '#') && context == Context.UrlParser -> {
                        pushPending(buffer.toString(), context)
                        currentInput = inputBeforeC
                        break
                    }

                    else -> {
                        checkUrlCodePoint(c, currentInput)
                        if (schemeType.isFile() &&
                            serialization.length > pathStart &&
                            isNormalizedWindowsDriveLetter(serialization.substring(pathStart + 1))
                        ) {
                            pushPending(buffer.toString(), context)
                            buffer.clear()
                            serialization.append('/')
                            segmentStart += 1
                        }
                        buffer.append(c)
                    }
                }
            }

            val segmentBeforeSlash =
                if (endsWithSlash) {
                    serialization.substring(segmentStart, serialization.length - 1)
                } else {
                    serialization.substring(segmentStart)
                }

            when (segmentBeforeSlash) {
                "..", "%2e%2e", "%2e%2E", "%2E%2e", "%2E%2E", "%2e.", "%2E.", ".%2e", ".%2E" -> {
                    serialization.setLength(segmentStart)
                    if (serialization.endsWith('/') && lastSlashCanBeRemoved(serialization.toString(), pathStart)) {
                        serialization.deleteAt(serialization.length - 1)
                    }
                    shortenPath(schemeType, pathStart)
                    if (endsWithSlash && !serialization.endsWith('/')) {
                        serialization.append('/')
                    }
                }

                ".", "%2e", "%2E" -> {
                    serialization.setLength(segmentStart)
                    if (!serialization.endsWith('/')) {
                        serialization.append('/')
                    }
                }

                else -> {
                    if (schemeType.isFile() && segmentStart == pathStart + 1 && isWindowsDriveLetter(segmentBeforeSlash)) {
                        val firstChar = segmentBeforeSlash[0]
                        serialization.setLength(segmentStart)
                        serialization.append(firstChar)
                        serialization.append(':')
                        if (endsWithSlash) {
                            serialization.append('/')
                        }
                        if (hasHost[0]) {
                            logViolation(SyntaxViolation.FileWithHostAndWindowsDrive)
                            hasHost[0] = false
                        }
                    }
                }
            }
            if (!endsWithSlash) {
                break
            }
        }

        if (schemeType.isFile()) {
            val path = serialization.substring(pathStart)
            serialization.setLength(pathStart)
            serialization.append('/')
            serialization.append(path.trimStart('/'))
        }

        return currentInput
    }

    public fun shortenPath(
        schemeType: SchemeType,
        pathStart: Int,
    ) {
        if (serialization.length == pathStart) return
        if (schemeType.isFile() && isNormalizedWindowsDriveLetter(serialization.substring(pathStart))) {
            return
        }
        popPath(schemeType, pathStart)
    }

    public fun popPath(
        schemeType: SchemeType,
        pathStart: Int,
    ) {
        if (serialization.length > pathStart) {
            val slashPosition = serialization.substring(pathStart).lastIndexOf('/')
            if (slashPosition >= 0) {
                val segmentStart = pathStart + slashPosition + 1
                if (!(schemeType.isFile() && isNormalizedWindowsDriveLetter(serialization.substring(segmentStart)))) {
                    serialization.setLength(segmentStart)
                }
            }
        }
    }

    public fun parseCannotBeABasePath(input: Input): Input {
        val current = input
        while (true) {
            val inputBeforeC = current.clone()
            val pair = current.nextUtf8() ?: return current
            val c = pair.first
            val utf8C = pair.second
            if ((c == '?' || c == '#') && context == Context.UrlParser) {
                return inputBeforeC
            }
            checkUrlCodePoint(c, current)
            serialization.append(PercentEncoding.utf8PercentEncode(utf8C, PercentEncoding::shouldEncodeControls))
        }
    }

    internal fun withQueryAndFragment(
        schemeType: SchemeType,
        schemeEnd: Int,
        usernameEnd: Int,
        hostStart: Int,
        hostEnd: Int,
        host: HostInternal,
        port: Int?,
        pathStartParam: Int,
        remaining: Input,
    ): ParseResult<Url> {
        var pathStart = pathStartParam
        if (pathStart == schemeEnd + 1) {
            if (serialization.substring(pathStart).startsWith("//")) {
                serialization.insert(pathStart, "/.")
                pathStart += 2
            }
        } else if (pathStart == schemeEnd + 3 && serialization.substring(schemeEnd, pathStart) == ":/.") {
            if (pathStart + 1 < serialization.length && serialization[pathStart + 1] != '/') {
                serialization.deleteRange(schemeEnd, pathStart)
                serialization.insert(schemeEnd, ":")
                pathStart -= 2
            }
        }

        val (queryStart, fragmentStart) =
            parseQueryAndFragment(schemeType, schemeEnd, remaining).getOrThrow()
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

    public fun parseQueryAndFragment(
        schemeType: SchemeType,
        schemeEnd: Int,
        input: Input,
    ): ParseResult<Pair<Int?, Int?>> {
        var currentInput = input
        var queryStart: Int? = null
        val (first, afterFirst) = currentInput.splitFirst()
        when (first) {
            '#' -> {}

            '?' -> {
                queryStart = toU32(serialization.length).getOrThrow()
                serialization.append('?')
                val remaining = parseQuery(schemeType, schemeEnd, afterFirst)
                if (remaining != null) {
                    currentInput = remaining
                } else {
                    return Result.success(Pair(queryStart, null))
                }
            }

            null -> {
                return Result.success(Pair(null, null))
            }

            else -> {}
        }

        val fragmentStart = toU32(serialization.length).getOrThrow()
        serialization.append('#')
        val afterHash = if (first == '#') afterFirst else currentInput
        parseFragment(afterHash)
        return Result.success(Pair(queryStart, fragmentStart))
    }

    internal class QueryPartIter(
        val isUrlParser: Boolean,
        val input: Input,
        val violationFn: ((SyntaxViolation) -> Unit)?,
    ) : Iterator<Pair<String, Boolean>> {
        public typealias Item = Pair<String, Boolean>

        private var nextItem: Pair<String, Boolean>? = null
        private var isFinished = false

        override fun hasNext(): Boolean {
            if (nextItem != null) return true
            if (isFinished) return false
            advance()
            return nextItem != null
        }

        override fun next(): Pair<String, Boolean> {
            if (!hasNext()) throw NoSuchElementException()
            val item = nextItem!!
            nextItem = null
            return item
        }

        private fun advance() {
            val buffer = StringBuilder()
            while (input.hasNext()) {
                val c = input.next()
                if (asciiTabOrNewLine(c)) {
                    nextItem = Pair(buffer.toString(), false)
                    return
                } else if (c == '#' && isUrlParser) {
                    nextItem = Pair(buffer.toString(), true)
                    isFinished = true
                    return
                } else {
                    violationFn?.let { vfn -> checkUrlCodePoint(vfn, c, input) }
                    buffer.append(c)
                }
            }
            if (buffer.isNotEmpty()) {
                nextItem = Pair(buffer.toString(), false)
            }
            isFinished = true
        }
    }

    public fun parseQuery(
        schemeType: SchemeType,
        schemeEnd: Int,
        input: Input,
    ): Input? {
        val queryPredicate =
            if (schemeType.isSpecial()) {
                PercentEncoding::shouldEncodeSpecialQuery
            } else {
                PercentEncoding::shouldEncodeQuery
            }

        val queryEncodingOverrideValid =
            if (queryEncodingOverride != null) {
                val scheme = serialization.substring(0, schemeEnd)
                scheme in arrayOf("http", "https", "file", "ftp")
            } else {
                false
            }

        val partIter = QueryPartIter(context == Context.UrlParser, input, violationFn)
        while (partIter.hasNext()) {
            val (part, isFinished) = partIter.next()
            if (part.isNotEmpty()) {
                val encoded =
                    if (queryEncodingOverrideValid && queryEncodingOverride != null) {
                        PercentEncoding.utf8PercentEncode(queryEncodingOverride(part), queryPredicate)
                    } else {
                        PercentEncoding.utf8PercentEncode(part, queryPredicate)
                    }
                serialization.append(encoded)
            }
            if (isFinished) {
                return partIter.input
            }
        }
        return null
    }

    public fun fragmentOnly(
        baseUrl: Url,
        input: Input,
    ): ParseResult<Url> {
        val beforeFragment =
            if (baseUrl.fragmentStart != null) {
                baseUrl.serialization.substring(0, baseUrl.fragmentStart!!)
            } else {
                baseUrl.serialization
            }
        serialization.setLength(0)
        serialization.append(beforeFragment)
        serialization.append('#')
        val afterHash = input.splitPrefix('#') ?: input
        parseFragment(afterHash)
        return Result.success(
            Url(
                serialization = serialization.toString(),
                schemeEnd = baseUrl.schemeEnd,
                usernameEnd = baseUrl.usernameEnd,
                hostStart = baseUrl.hostStart,
                hostEnd = baseUrl.hostEnd,
                host = baseUrl.host,
                port = baseUrl.port,
                pathStart = baseUrl.pathStart,
                queryStart = baseUrl.queryStart,
                fragmentStart = toU32(beforeFragment.length).getOrThrow(),
            ),
        )
    }

    internal class FragmentPartIter(
        val input: Input,
        val violationFn: ((SyntaxViolation) -> Unit)?,
    ) : Iterator<String> {
        public typealias Item = String

        private var nextItem: String? = null
        private var isFinished = false

        override fun hasNext(): Boolean {
            if (nextItem != null) return true
            if (isFinished) return false
            advance()
            return nextItem != null
        }

        override fun next(): String {
            if (!hasNext()) throw NoSuchElementException()
            val item = nextItem!!
            nextItem = null
            return item
        }

        private fun advance() {
            val buffer = StringBuilder()
            while (input.hasNext()) {
                val c = input.next()
                if (asciiTabOrNewLine(c)) {
                    nextItem = buffer.toString()
                    return
                } else if (c == '\u0000') {
                    violationFn?.invoke(SyntaxViolation.NullInFragment)
                } else {
                    violationFn?.let { vfn -> checkUrlCodePoint(vfn, c, input) }
                    buffer.append(c)
                }
            }
            if (buffer.isNotEmpty()) {
                nextItem = buffer.toString()
            }
            isFinished = true
        }
    }

    public fun parseFragment(input: Input) {
        val partIter = FragmentPartIter(input, violationFn)
        while (partIter.hasNext()) {
            val part = partIter.next()
            if (part.isNotEmpty()) {
                serialization.append(PercentEncoding.utf8PercentEncode(part, PercentEncoding::shouldEncodeFragment))
            }
        }
    }

    public fun logViolation(violation: SyntaxViolation) {
        violationFn?.invoke(violation)
    }

    public fun logViolationIf(
        violation: SyntaxViolation,
        condition: () -> Boolean,
    ) {
        if (violationFn != null && condition()) {
            violationFn(violation)
        }
    }

    public fun checkUrlCodePoint(
        c: Char,
        input: Input,
    ) {
        if (violationFn != null) {
            checkUrlCodePoint(violationFn, c, input)
        }
    }
}

internal fun checkUrlCodePoint(
    vfn: (SyntaxViolation) -> Unit,
    c: Char,
    input: Input,
) {
    if (c == '%') {
        val nextInput = input.clone()
        val a = nextInput.nextOrNull()
        val b = nextInput.nextOrNull()
        if (a == null ||
            b == null ||
            (!a.isDigit() && a !in 'a'..'f' && a !in 'A'..'F') ||
            (!b.isDigit() && b !in 'a'..'f' && b !in 'A'..'F')
        ) {
            vfn(SyntaxViolation.PercentDecode)
        }
    } else if (!isUrlCodePoint(c)) {
        vfn(SyntaxViolation.NonUrlCodePoint)
    }
}

public fun isUrlCodePoint(c: Char): Boolean =
    when (c) {
        in 'a'..'z', in 'A'..'Z', in '0'..'9' -> true
        '!', '$', '&', '\'', '(', ')', '*', '+', ',', '-', '.', '/', ':', ';', '=', '?', '@', '_', '~' -> true
        in '\u00A0'..'\uD7FF', in '\uE000'..'\uFDCF', in '\uFDF0'..'\uFFFD' -> true
        else -> false
    }

public fun c0ControlOrSpace(ch: Char): Boolean = ch <= ' '

public fun asciiTabOrNewLine(ch: Char): Boolean = ch == '\t' || ch == '\n' || ch == '\r'

public fun asciiAlpha(ch: Char): Boolean = ch in 'a'..'z' || ch in 'A'..'Z'

public fun toU32(i: Int): ParseResult<Int> =
    if (i >= 0) {
        Result.success(i)
    } else {
        Result.failure(ParseError.Overflow)
    }

public fun isNormalizedWindowsDriveLetter(segment: String): Boolean =
    isWindowsDriveLetter(segment) && segment.length >= 2 && segment[1] == ':'

public fun isWindowsDriveLetter(segment: String): Boolean =
    segment.length == 2 && startsWithWindowsDriveLetter(segment)

public fun pathStartsWithWindowsDriveLetter(s: String): Boolean =
    if (s.isNotEmpty()) {
        val c = s[0]
        (c == '/' || c == '\\' || c == '?' || c == '#') && startsWithWindowsDriveLetter(s.substring(1))
    } else {
        false
    }

public fun startsWithWindowsDriveLetter(s: String): Boolean =
    s.length >= 2 &&
        asciiAlpha(s[0]) &&
        (s[1] == ':' || s[1] == '|') &&
        (s.length == 2 || s[2] == '/' || s[2] == '\\' || s[2] == '?' || s[2] == '#')

internal fun startsWithWindowsDriveLetterSegment(input: Input): Boolean {
    val nextInput = input.clone()
    val a = nextInput.nextOrNull()
    val b = nextInput.nextOrNull()
    val c = nextInput.nextOrNull()
    return when {
        a != null && b != null && c != null && asciiAlpha(a) && (b == ':' || b == '|') && (c == '/' || c == '\\' || c == '?' || c == '#') -> true
        a != null && b != null && c == null && asciiAlpha(a) && (b == ':' || b == '|') -> true
        else -> false
    }
}

public fun fastU16ToStr(value: Int): String = value.toString()

internal object PercentEncoding {
    private val hexDigits = "0123456789ABCDEF".toCharArray()

    fun shouldEncodeControls(byte: Int): Boolean = byte in 0x00..0x1F || byte >= 0x7F

    fun shouldEncodeFragment(byte: Int): Boolean =
        shouldEncodeControls(byte) || byte == ' '.code || byte == '"'.code || byte == '<'.code || byte == '>'.code || byte == '`'.code

    fun shouldEncodeQuery(byte: Int): Boolean =
        shouldEncodeControls(byte) || byte == ' '.code || byte == '"'.code || byte == '#'.code || byte == '<'.code || byte == '>'.code

    fun shouldEncodeSpecialQuery(byte: Int): Boolean =
        shouldEncodeQuery(byte) || byte == '\''.code

    fun shouldEncodePath(byte: Int): Boolean =
        shouldEncodeFragment(byte) || byte == '#'.code || byte == '?'.code || byte == '{'.code || byte == '}'.code

    fun shouldEncodePathSegment(byte: Int): Boolean =
        shouldEncodePath(byte) || byte == '/'.code || byte == '%'.code

    fun shouldEncodeSpecialPathSegment(byte: Int): Boolean =
        shouldEncodePathSegment(byte) || byte == '\\'.code

    fun shouldEncodeUserInfo(byte: Int): Boolean =
        shouldEncodePath(byte) ||
            byte == '/'.code ||
            byte == ':'.code ||
            byte == ';'.code ||
            byte == '='.code ||
            byte == '@'.code ||
            byte == '['.code ||
            byte == '\\'.code ||
            byte == ']'.code ||
            byte == '^'.code ||
            byte == '|'.code

    fun utf8PercentEncode(
        text: String,
        predicate: (Int) -> Boolean,
    ): String {
        val bytes = text.encodeToByteArray()
        val sb = StringBuilder(bytes.size)
        for (b in bytes) {
            val unsignedByte = b.toInt() and 0xFF
            if (predicate(unsignedByte)) {
                sb.append('%')
                sb.append(hexDigits[unsignedByte ushr 4])
                sb.append(hexDigits[unsignedByte and 0x0F])
            } else {
                sb.append(unsignedByte.toChar())
            }
        }
        return sb.toString()
    }
}
