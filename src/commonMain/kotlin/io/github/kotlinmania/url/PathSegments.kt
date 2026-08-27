// port-lint: source path_segments.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.url

import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.native.HiddenFromObjC

@HiddenFromObjC
public class PathSegmentsMut(
    private val url: Url,
) {
    public fun clear(): PathSegmentsMut {
        url.setPath("/")
        return this
    }

    public fun push(segment: String): PathSegmentsMut {
        val encodedSegment = if (url.isSpecial()) segment.replace("\\", "%5C") else segment
        val currentPath = url.path()
        val newPath = if (currentPath.endsWith("/")) "$currentPath$encodedSegment" else "$currentPath/$encodedSegment"
        url.setPath(newPath)
        return this
    }

    public fun pop(): PathSegmentsMut {
        val currentPath = url.path()
        val lastSlash = currentPath.lastIndexOf('/')
        val newPath = if (lastSlash > 0) currentPath.substring(0, lastSlash) else "/"
        url.setPath(newPath)
        return this
    }

    public fun segments(): List<String> = url.pathSegments() ?: emptyList()

    public fun popIfEmpty(): PathSegmentsMut {
        val currentPath = url.path()
        if (currentPath.endsWith("/") && currentPath.length > 1) {
            url.setPath(currentPath.removeSuffix("/"))
        }
        return this
    }

    public fun extend(segments: Iterable<String>): PathSegmentsMut {
        for (segment in segments) {
            if (segment == "." || segment == "..") continue
            push(segment)
        }
        return this
    }

    public fun extend(segments: Array<out String>): PathSegmentsMut =
        extend(segments.asIterable())
}
