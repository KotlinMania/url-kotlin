// port-lint: source url/src/origin.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.url

import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.native.HiddenFromObjC

private var counter: Long = 0L

@HiddenFromObjC
public data class OpaqueOrigin(
    public val id: Long,
)

@HiddenFromObjC
public sealed class Origin {
    public data class Tuple(
        public val scheme: String,
        public val host: Host<String>,
        public val port: Int,
    ) : Origin()

    public data class Opaque(
        public val opaque: OpaqueOrigin,
    ) : Origin()

    public companion object {
        public fun newOpaque(): Origin = Opaque(OpaqueOrigin(++counter))
    }

    public fun isTuple(): Boolean = this is Tuple

    public fun asciiSerialization(): String =
        when (this) {
            is Opaque -> "null"
            is Tuple -> {
                val hostStr = host.toString()
                if (defaultPort(scheme) == port) {
                    "$scheme://$hostStr"
                } else {
                    "$scheme://$hostStr:$port"
                }
            }
        }

    public fun unicodeSerialization(): String =
        when (this) {
            is Opaque -> "null"
            is Tuple -> {
                val hostStr =
                    when (host) {
                        is Host.Domain -> domainToUnicode(host.domain)
                        else -> host.toString()
                    }
                if (defaultPort(scheme) == port) {
                    "$scheme://$hostStr"
                } else {
                    "$scheme://$hostStr:$port"
                }
            }
        }

    override fun toString(): String = asciiSerialization()
}

public fun urlOrigin(url: Url): Origin {
    val scheme = url.scheme()
    return when (scheme) {
        "blob" -> {
            val result = Url.parse(url.path())
            if (result.isSuccess) {
                urlOrigin(result.getOrThrow())
            } else {
                Origin.newOpaque()
            }
        }
        "ftp", "http", "https", "ws", "wss" -> {
            val host = url.host()
            val port = url.portOrKnownDefault()
            if (host != null && port != null) {
                Origin.Tuple(scheme, host, port)
            } else {
                Origin.newOpaque()
            }
        }
        "file" -> Origin.newOpaque()
        else -> Origin.newOpaque()
    }
}
