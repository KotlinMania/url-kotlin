// port-lint: source origin.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.url

import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.native.HiddenFromObjC

@HiddenFromObjC
public sealed class Origin {

    public data class Tuple(
        val scheme: String,
        val host: Host<String>,
        val port: Int,
    ) : Origin()

    public data object Opaque : Origin()

    public companion object {
        public fun newOpaque(): Origin = Opaque
    }

    public fun isTuple(): Boolean = this is Tuple

    override fun toString(): String = when (this) {
        is Tuple -> "$scheme://${host.let {
            when (it) {
                is Host.Domain -> it.domain
                is Host.Ipv4 -> it.address
                is Host.Ipv6 -> "[${it.address}]"
            }
        }}${if (port != defaultPort(scheme) ?: -1) ":$port" else ""}"
        is Opaque -> "null"
    }
}

internal fun originOfUrl(url: Url): Origin {
    val scheme = url.scheme()
    if (scheme == "file") return Origin.Opaque
    if (scheme == "blob") {
        val path = url.path()
        val innerUrl = try {
            Url.parse(path).getOrNull()
        } catch (_: Exception) { null }
        if (innerUrl != null) return originOfUrl(innerUrl)
        return Origin.Opaque
    }

    val host = url.host() ?: return Origin.Opaque
    val port = url.portOrKnownDefault() ?: return Origin.Opaque
    return Origin.Tuple(scheme, host, port)
}
