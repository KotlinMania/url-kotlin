// port-lint: source host.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.url

import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.native.HiddenFromObjC
import kotlin.math.pow

@HiddenFromObjC
public sealed class Host<out T> {

    public data class Domain<T>(val domain: T) : Host<T>()

    public data class Ipv4(val address: String) : Host<Nothing>()

    public data class Ipv6(val address: String) : Host<Nothing>()

    internal fun toInternal(): HostInternal = when (this) {
        is Domain<*> -> HostInternal.Domain
        is Ipv4 -> HostInternal.Ipv4(address)
        is Ipv6 -> HostInternal.Ipv6(address)
    }

    override fun toString(): String = when (this) {
        is Domain<*> -> domain.toString()
        is Ipv4 -> address
        is Ipv6 -> "[$address]"
    }

    override fun equals(other: Any?): Boolean = when {
        other !is Host<*> -> false
        this is Domain<*> && other is Domain<*> -> domain == other.domain
        this is Ipv4 && other is Ipv4 -> address == other.address
        this is Ipv6 && other is Ipv6 -> address == other.address
        else -> false
    }

    override fun hashCode(): Int = when (this) {
        is Domain<*> -> domain.hashCode()
        is Ipv4 -> address.hashCode()
        is Ipv6 -> address.hashCode()
    }

    public companion object {
        public fun parse(host: String): Result<Host<String>> {
            if (host.startsWith("[") && host.endsWith("]")) {
                val inner = host.substring(1, host.length - 1)
                if (isValidIpv6(inner)) return Result.success(Ipv6(inner))
                return Result.failure(UrlError.InvalidIpv6Address)
            }
            if (isValidIpv4(host)) return Result.success(Ipv4(host))
            if (isValidDomain(host)) return Result.success(Domain(host.lowercase()))
            return Result.failure(UrlError.InvalidDomainCharacter)
        }

        public fun parseOpaque(host: String): Host<String> {
            if (host.startsWith("[") && host.endsWith("]")) {
                val inner = host.substring(1, host.length - 1)
                if (isValidIpv6(inner)) return Ipv6(inner)
            }
            return Domain(host)
        }
    }
}

internal sealed class HostInternal {
    data object None : HostInternal()
    data object Domain : HostInternal()
    data class Ipv4(val address: String) : HostInternal()
    data class Ipv6(val address: String) : HostInternal()

    override fun equals(other: Any?): Boolean = when {
        other !is HostInternal -> false
        this is None && other is None -> true
        this is Domain && other is Domain -> true
        this is Ipv4 && other is Ipv4 -> address == other.address
        this is Ipv6 && other is Ipv6 -> address == other.address
        else -> false
    }

    override fun hashCode(): Int = when (this) {
        is None -> 0
        is Domain -> 1
        is Ipv4 -> address.hashCode()
        is Ipv6 -> address.hashCode()
    }
}

internal fun parseIpv4Number(s: String): Int? = when {
    s.startsWith("0x") || s.startsWith("0X") -> {
        val hex = s.substring(2)
        if (hex.isEmpty()) null else hex.toIntOrNull(16)
    }
    s.startsWith("0") && s.length > 1 -> {
        if (s.any { it !in '0'..'7' }) null else s.substring(1).toIntOrNull(8)
    }
    s.firstOrNull() == '+' || s.firstOrNull() == '-' -> null
    else -> s.toIntOrNull()
}

internal fun parseIpv4Addr(s: String): String? {
    val parts = s.split('.')
    if (parts.size > 4 || parts.any { it.isEmpty() }) return null
    val nums = parts.map { parseIpv4Number(it) ?: return null }
    val addr: Long = when (parts.size) {
        1 -> if (nums[0] > 0xFFFFFFFF) return null else nums[0].toLong() and 0xFFFFFFFFL
        2 -> if (nums[0] > 0xFF || nums[1] > 0xFFFFFF) return null
             else (nums[0].toLong() shl 24) or nums[1].toLong()
        3 -> if (nums[0] > 0xFF || nums[1] > 0xFF || nums[2] > 0xFFFF) return null
             else (nums[0].toLong() shl 24) or (nums[1].toLong() shl 16) or nums[2].toLong()
        4 -> if (nums.any { it > 0xFF }) return null
             else (nums[0].toLong() shl 24) or (nums[1].toLong() shl 16) or (nums[2].toLong() shl 8) or nums[3].toLong()
        else -> return null
    }
    return "${(addr shr 24) and 0xFF}.${(addr shr 16) and 0xFF}.${(addr shr 8) and 0xFF}.${addr and 0xFF}"
}

internal fun isValidIpv4(s: String): Boolean = parseIpv4Addr(s) != null

internal fun isValidIpv6(s: String): Boolean {
    if (s.isEmpty()) return false
    if (s.count { it == ':' } > 7) return false
    if (s.startsWith("::") || s.endsWith("::")) return true
    if (s.contains("::")) {
        val parts = s.split("::")
        if (parts.size != 2) return false
    }
    val groups = s.split(':').filter { it.isNotEmpty() }
    if (groups.size > 8) return false
    return groups.all { it.length <= 4 && it.all { c -> c in '0'..'9' || c in 'a'..'f' || c in 'A'..'F' } }
}

internal fun isValidDomain(s: String): Boolean {
    if (s.isEmpty()) return true
    if (s.contains("..")) return false
    val labels = s.split('.')
    return labels.all { label ->
        label.isNotEmpty() ||
            (label.isEmpty() && labels.first() == label)  // allow leading dot
    } && labels.withIndex().all { (i, label) ->
        label.isEmpty() || (
            label.all { it.isLetterOrDigit() || it == '-' } &&
                !label.startsWith('-') &&
                !label.endsWith('-')
        )
    }
}

internal fun expandIpv6(short: String): String {
    val parts = short.split("::", limit = 2)
    val left = if (parts[0].isEmpty()) emptyList() else parts[0].split(':').filter { it.isNotEmpty() }
    val right = if (parts.size > 1 && parts[1].isNotEmpty()) parts[1].split(':').filter { it.isNotEmpty() } else emptyList()
    val groups = mutableListOf<String>()
    for (g in left) groups.add(g.padStart(4, '0').lowercase())
    val fillCount = 8 - left.size - right.size
    for (i in 0 until fillCount) groups.add("0000")
    for (g in right) groups.add(g.padStart(4, '0').lowercase())
    return groups.joinToString(":")
}

internal fun shortenIpv6(expanded: String): String {
    val groups = expanded.split(':')
    val (bestStart, bestLen) = longestZeroSequence(groups)
    fun trim(g: String) = g.trimStart('0').let { if (it.isEmpty()) "0" else it }
    if (bestLen < 2) return groups.joinToString(":") { trim(it) }
    val before = groups.take(bestStart)
    val after = groups.drop(bestStart + bestLen)
    return buildString {
        if (before.isNotEmpty()) append(before.joinToString(":") { trim(it) })
        append("::")
        if (after.isNotEmpty()) append(after.joinToString(":") { trim(it) })
    }
}

internal fun longestZeroSequence(groups: List<String>): Pair<Int, Int> {
    var bestStart = -1
    var bestLen = 1
    var i = 0
    while (i < groups.size) {
        if (groups[i] == "0000") {
            var j = i
            while (j < groups.size && groups[j] == "0000") j++
            val len = j - i
            if (len > bestLen) { bestLen = len; bestStart = i }
            i = j
        } else { i++ }
    }
    return Pair(bestStart, bestLen)
}

internal fun endsInANumber(s: String): Boolean {
    val lastDot = s.lastIndexOf('.')
    val lastLabel = if (lastDot >= 0) s.substring(lastDot + 1) else s
    if (lastLabel.isEmpty()) return false
    return (parseIpv4Number(lastLabel) != null)
}

internal fun parseIpv6Addr(input: String): Result<String> {
    if (input.isEmpty()) return Result.failure(UrlError.InvalidIpv6Address)
    if (input.count { it == ':' } > 7) return Result.failure(UrlError.InvalidIpv6Address)
    val hasDoubleColon = "::" in input
    if (hasDoubleColon && input.split("::").size > 2) return Result.failure(UrlError.InvalidIpv6Address)

    fun parseHexPiece(s: String): String? {
        if (s.isEmpty() || s.length > 4) return null
        if (s.any { c -> !c.isDigit() && c !in 'a'..'f' && c !in 'A'..'F' }) return null
        return s.padStart(4, '0').lowercase()
    }

    val parts = input.split("::", limit = 2)
    val leftPieces = if (parts[0].isNotEmpty()) parts[0].split(':') else emptyList()
    val rightPieces = if (parts.size > 1 && parts[1].isNotEmpty()) parts[1].split(':') else emptyList()

    val leftGroups = mutableListOf<String>()
    for (piece in leftPieces) {
        leftGroups.add(parseHexPiece(piece) ?: return Result.failure(UrlError.InvalidIpv6Address))
    }

    val rightHexGroups = mutableListOf<String>()
    var ipv4Groups = mutableListOf<String>()

    if (rightPieces.isNotEmpty()) {
        val last = rightPieces.last()
        if (last.contains('.')) {
            val ipv4Addr = parseIpv4Addr(last) ?: return Result.failure(UrlError.InvalidIpv6Address)
            val nums = ipv4Addr.split('.').map { it.toInt() }
            ipv4Groups.add((nums[0] * 256 + nums[1]).toString(16).padStart(4, '0').lowercase())
            ipv4Groups.add((nums[2] * 256L + nums[3]).toString(16).padStart(4, '0').lowercase())
            for (piece in rightPieces.dropLast(1)) {
                rightHexGroups.add(parseHexPiece(piece) ?: return Result.failure(UrlError.InvalidIpv6Address))
            }
        } else {
            for (piece in rightPieces) {
                rightHexGroups.add(parseHexPiece(piece) ?: return Result.failure(UrlError.InvalidIpv6Address))
            }
        }
    }

    val totalGroups = leftGroups.size + rightHexGroups.size + ipv4Groups.size
    val fillCount = 8 - totalGroups

    if (fillCount < 0) return Result.failure(UrlError.InvalidIpv6Address)
    if (fillCount > 0 && !hasDoubleColon) return Result.failure(UrlError.InvalidIpv6Address)

    val allGroups = mutableListOf<String>()
    allGroups.addAll(leftGroups)
    for (i in 0 until fillCount) allGroups.add("0000")
    allGroups.addAll(rightHexGroups)
    allGroups.addAll(ipv4Groups)

    if (allGroups.size != 8) return Result.failure(UrlError.InvalidIpv6Address)

    return Result.success(allGroups.joinToString(":"))
}
