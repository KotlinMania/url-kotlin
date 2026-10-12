// port-lint: source path_segments.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.url

import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.native.HiddenFromObjC

/**
 * Exposes methods to manipulate the path of an URL that is not cannot-be-base.
 *
 * The path always starts with a `/` slash, and is made of slash-separated segments.
 * There is always at least one segment (which may be the empty string).
 */
@HiddenFromObjC
public class PathSegmentsMut(
    private val url: Url,
) {
    /**
     * Remove all segments in the path, leaving the minimal `url.path() == "/"`.
     *
     * Returns this instance so that method calls can be chained.
     */
    public fun clear(): PathSegmentsMut {
        url.setPath("/")
        return this
    }

    /**
     * Push the given segment onto the end of the path.
     */
    public fun push(segment: String): PathSegmentsMut {
        val encodedSegment = if (url.isSpecial()) segment.replace("\\", "%5C") else segment
        val currentPath = url.path()
        val newPath = if (currentPath.endsWith("/")) "$currentPath$encodedSegment" else "$currentPath/$encodedSegment"
        url.setPath(newPath)
        return this
    }

    /**
     * Remove the last segment of the path.
     */
    public fun pop(): PathSegmentsMut {
        val currentPath = url.path()
        val lastSlash = currentPath.lastIndexOf('/')
        val newPath = if (lastSlash > 0) currentPath.substring(0, lastSlash) else "/"
        url.setPath(newPath)
        return this
    }

    /**
     * Return all current path segments as a list.
     */
    public fun segments(): List<String> = url.pathSegments().orEmpty()

    /**
     * Remove the last segment of the path if it is empty.
     */
    public fun popIfEmpty(): PathSegmentsMut {
        val currentPath = url.path()
        if (currentPath.endsWith("/") && currentPath.length > 1) {
            url.setPath(currentPath.removeSuffix("/"))
        }
        return this
    }

    /**
     * Append all segments in the given iterable onto the end of the path.
     */
    public fun extend(segments: Iterable<String>): PathSegmentsMut {
        for (segment in segments) {
            if (segment == "." || segment == "..") continue
            push(segment)
        }
        return this
    }

    /**
     * Append all segments in the given array onto the end of the path.
     */
    public fun extend(segments: Array<out String>): PathSegmentsMut =
        extend(segments.asIterable())

    /**
     * Restores after-path position and state on drop.
     */
    public fun drop() {
    }

    public companion object {
        public fun new(url: Url): PathSegmentsMut = PathSegmentsMut(url)
    }
}
