// port-lint: source parser.rs
package io.github.kotlinmania.url

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

    public fun errorDescription(): String = details
}

public typealias UrlError = ParseError

public typealias ParseResult<T> = Result<T>

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
