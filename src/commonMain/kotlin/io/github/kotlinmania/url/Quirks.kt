// port-lint: source quirks.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.url

import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.native.HiddenFromObjC

@HiddenFromObjC
public data class InternalComponents(
    public val schemeEnd: Int,
    public val usernameEnd: Int,
    public val hostStart: Int,
    public val hostEnd: Int,
    public val port: Int?,
    public val pathStart: Int,
    public val queryStart: Int?,
    public val fragmentStart: Int?,
)

public fun internalComponents(url: Url): InternalComponents =
    InternalComponents(
        schemeEnd = url.schemeEnd,
        usernameEnd = url.usernameEnd,
        hostStart = url.hostStart,
        hostEnd = url.hostEnd,
        port = url.port,
        pathStart = url.pathStart,
        queryStart = url.queryStart,
        fragmentStart = url.fragmentStart,
    )

internal object Quirks {
    fun isSpecialScheme(scheme: String): Boolean =
        scheme.lowercase() in specialSchemes

    fun defaultPort(scheme: String): Int? =
        io.github.kotlinmania.url
            .defaultPort(scheme)
}

internal fun setHref(url: Url, input: String): Result<Unit> {
    val parsed = Url.parse(input)
    if (parsed.isFailure) return Result.failure(parsed.exceptionOrNull() ?: ParseError.ParseFailed)
    val parsedUrl = parsed.getOrThrow()
    url.serialization = parsedUrl.serialization
    url.schemeEnd = parsedUrl.schemeEnd
    url.usernameEnd = parsedUrl.usernameEnd
    url.hostStart = parsedUrl.hostStart
    url.hostEnd = parsedUrl.hostEnd
    url.host = parsedUrl.host
    url.port = parsedUrl.port
    url.pathStart = parsedUrl.pathStart
    url.queryStart = parsedUrl.queryStart
    url.fragmentStart = parsedUrl.fragmentStart
    return Result.success(Unit)
}

fun domainToAscii(domain: String): String {
    val parsed = Host.parse(domain)
    if (parsed.isFailure) return ""
    val host = parsed.getOrThrow()
    return when (host) {
        is Host.Domain<*> -> host.domain as String
        else -> ""
    }
}

fun domainToUnicode(domain: String): String {
    val parsed = Host.parse(domain)
    if (parsed.isFailure) return ""
    val host = parsed.getOrThrow()
    return when (host) {
        is Host.Domain<*> -> host.domain as String
        else -> ""
    }
}

fun href(url: Url): String = url.asStr()

fun origin(url: Url): String = url.origin().toString()

fun protocol(url: Url): String = url.scheme() + ":"

fun setProtocol(url: Url, newProtocol: String) {
    val clean = newProtocol.substringBefore(":")
    url.setScheme(clean)
}

fun username(url: Url): String = url.username()

fun setUsername(url: Url, newUsername: String) {
    url.setUsername(newUsername)
}

fun password(url: Url): String = url.password() ?: ""

fun setPassword(url: Url, newPassword: String) {
    url.setPassword(if (newPassword.isEmpty()) null else newPassword)
}

fun host(url: Url): String = url.hostStr() ?: ""

fun setHost(url: Url, newHost: String): Result<Unit> {
    if (url.cannotBeABase()) return Result.failure(ParseError.CannotSetHost)
    if (newHost.isEmpty()) {
        if (url.isSpecial() && url.scheme() != "file") return Result.failure(ParseError.EmptyHost)
        return url.setHost(newHost)
    }
    val hostOnly =
        if (!newHost.startsWith("[") || !newHost.endsWith("]")) {
            val colonIdx = newHost.indexOf(':')
            when {
                colonIdx == 0 -> return Result.failure(ParseError.InvalidDomainCharacter)
                colonIdx > 0 -> newHost.substring(0, colonIdx)
                else -> newHost
            }
        } else {
            newHost
        }

    val parsedHost = Host.parse(hostOnly)
    if (parsedHost.isFailure) return Result.failure(ParseError.InvalidDomainCharacter)

    val optPort: Int? =
        if (!newHost.startsWith("[") || !newHost.endsWith("]")) {
            val colonIdx = newHost.indexOf(':')
            if (colonIdx > 0) {
                val portStr = newHost.substring(colonIdx + 1)
                if (portStr.isEmpty()) null else portStr.toIntOrNull()
            } else {
                null
            }
        } else {
            null
        }

    val hostValue = parsedHost.getOrThrow()
    val domainStr =
        when (hostValue) {
            is Host.Domain<*> -> hostValue.domain as? String
            else -> null
        }
    if (domainStr == "") {
        val hasCreds = url.username().isNotEmpty() || (url.password()?.isNotEmpty() == true)
        if (hasCreds || optPort != null || url.port != null) return Result.failure(ParseError.EmptyHost)
    }

    url.setHostInternal(hostValue, optPort)
    return Result.success(Unit)
}

fun hostname(url: Url): String = url.hostStr() ?: ""

fun setHostname(url: Url, newHostname: String): Result<Unit> {
    if (url.cannotBeABase()) return Result.failure(ParseError.CannotSetHost)

    if (newHostname.isEmpty() && url.isSpecial() && url.scheme() != "file") {
        return Result.failure(ParseError.EmptyHost)
    }

    val parsedHost = Host.parse(newHostname)
    if (parsedHost.isFailure) return Result.failure(ParseError.InvalidDomainCharacter)

    val host = parsedHost.getOrThrow()
    val domainStr =
        when (host) {
            is Host.Domain<*> -> host.domain as? String
            else -> null
        }
    if (domainStr == "") {
        val schemeType = url.scheme()
        if (schemeType in specialSchemes && schemeType != "file") {
            return Result.failure(ParseError.EmptyHost)
        }
        if (url.port != null || url.username().isNotEmpty() || (url.password()?.isNotEmpty() == true)) {
            return Result.failure(ParseError.EmptyHost)
        }
    }

    url.setHostInternal(host, null)
    return Result.success(Unit)
}

fun port(url: Url): String = url.slice(Position.BeforePort, Position.AfterPort)

fun setPort(url: Url, newPort: String): Result<Unit> {
    if (!url.hasHost() || url.host() == Host.Domain("") || url.scheme() == "file") {
        return Result.failure(ParseError.InvalidPort)
    }
    val portNum = if (newPort.isEmpty()) null else newPort.toIntOrNull()
    if (portNum == null && newPort.isNotEmpty()) return Result.failure(ParseError.InvalidPort)

    val effectivePort = if (portNum != null && portNum == defaultPort(url.scheme())) null else portNum
    url.setPortInternal(effectivePort)
    return Result.success(Unit)
}

fun pathname(url: Url): String = url.path()

fun setPathname(url: Url, newPathname: String) {
    if (url.cannotBeABase()) return
    if (newPathname.startsWith("/") || (url.isSpecial() && newPathname.startsWith("\\"))) {
        url.setPath(newPathname)
    } else if (url.isSpecial() || newPathname.isNotEmpty() || !url.hasHost()) {
        url.setPath("/$newPathname")
    } else {
        url.setPath(newPathname)
    }
}

fun search(url: Url): String = trim(url.slice(Position.AfterPath, Position.AfterQuery))

fun setSearch(url: Url, newSearch: String) {
    url.setQuery(
        when {
            newSearch.isEmpty() -> null
            newSearch.startsWith("?") -> newSearch.substring(1)
            else -> newSearch
        },
    )
}

fun hash(url: Url): String = trim(url.slice(Position.AfterQuery, Position.AfterFragment))

fun setHash(url: Url, newHash: String) {
    url.setFragment(
        when {
            newHash.isEmpty() -> null
            newHash.startsWith("#") -> newHash.substring(1)
            else -> newHash
        },
    )
}

private fun trim(s: String): String = if (s.length == 1) "" else s
