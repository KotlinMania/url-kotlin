// port-lint: source parser.rs
package io.github.kotlinmania.url

public sealed class UrlError : Exception() {
    public data object InvalidIpv6Address : UrlError()
    public data object InvalidDomainCharacter : UrlError()
    public data object EmptyHost : UrlError()
    public data object RelativeUrlWithoutBase : UrlError()
    public data object CannotSetHost : UrlError()
    public data object SetHostOnCannotBeABaseUrl : UrlError()
    public data class NotSupported(val operation: String) : UrlError()
    public data object ParseFailed : UrlError()
    public data object InvalidPort : UrlError()
    public data class Other(override val message: String) : UrlError()
}

public enum class SyntaxViolation(val description: String) {
    ExpectedDoubleSlash("Expected double slash at start of authority"),
    PercentDecodeExpected("Percent-decoded bytes did not trigger a valid character"),
    NonCharacter("Code point is a non-character"),
    InvalidCodePoint("Code point is invalid in URL"),
}

public typealias EncodingOverride = ((String) -> String)?
