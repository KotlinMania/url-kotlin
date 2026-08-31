// port-lint: source url/src/slicing.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.url

import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.native.HiddenFromObjC

@HiddenFromObjC
public enum class Position {
    BeforeScheme,
    AfterScheme,
    BeforeUsername,
    AfterUsername,
    BeforePassword,
    AfterPassword,
    BeforeHost,
    AfterHost,
    BeforePort,
    AfterPort,
    BeforePath,
    AfterPath,
    BeforeQuery,
    AfterQuery,
    BeforeFragment,
    AfterFragment,
}

internal fun countDigits(n: Int): Int =
    when (n) {
        in 0..9 -> 1
        in 10..99 -> 2
        in 100..999 -> 3
        in 1000..9999 -> 4
        else -> 5
    }

public fun Url.index(position: Position): Int =
    when (position) {
        Position.BeforeScheme -> 0
        Position.AfterScheme -> schemeEnd
        Position.BeforeUsername -> {
            if (hasAuthority()) {
                schemeEnd + "://".length
            } else {
                schemeEnd + ":".length
            }
        }
        Position.AfterUsername -> usernameEnd
        Position.BeforePassword -> {
            if (hasAuthority() && serialization.getOrNull(usernameEnd) == ':') {
                usernameEnd + 1
            } else {
                usernameEnd
            }
        }
        Position.AfterPassword -> {
            if (hasAuthority() && serialization.getOrNull(usernameEnd) == ':') {
                hostStart - 1
            } else {
                hostStart
            }
        }
        Position.BeforeHost -> hostStart
        Position.AfterHost -> hostEnd
        Position.BeforePort -> {
            if (port != null) {
                hostEnd + 1
            } else {
                hostEnd
            }
        }
        Position.AfterPort -> {
            if (port != null) {
                hostEnd + 1 + countDigits(port!!)
            } else {
                hostEnd
            }
        }
        Position.BeforePath -> pathStart
        Position.AfterPath -> {
            when {
                queryStart != null -> queryStart!!
                fragmentStart != null -> fragmentStart!!
                else -> serialization.length
            }
        }
        Position.BeforeQuery -> {
            when {
                queryStart != null -> queryStart!! + 1
                fragmentStart != null -> fragmentStart!!
                else -> serialization.length
            }
        }
        Position.AfterQuery -> {
            fragmentStart ?: serialization.length
        }
        Position.BeforeFragment -> {
            if (fragmentStart != null) {
                fragmentStart!! + 1
            } else {
                serialization.length
            }
        }
        Position.AfterFragment -> serialization.length
    }

public operator fun Url.get(start: Position, end: Position): String =
    serialization.substring(index(start), index(end))

public operator fun Url.get(range: ClosedRange<Position>): String =
    serialization.substring(index(range.start), index(range.endInclusive))
