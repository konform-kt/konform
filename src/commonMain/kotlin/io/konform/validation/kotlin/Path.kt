package io.konform.validation.kotlin

import kotlin.reflect.KFunction1
import kotlin.reflect.KProperty1

/** Represents a JSONPath-ish path to a property. */
internal object Path {
    /** Get a path, but treat a single string as the full path */
    fun asPathOrToPath(vararg segments: Any): String =
        if (segments.size == 1 && segments[0] is String) segments[0] as String
        else toPath(*segments)

    fun toPath(vararg segments: Any): String = segments.joinToString("") { toPathSegment(it) }

    fun toPathSegment(it: Any): String =
        when (it) {
            is KProperty1<*, *> -> ".${it.name}"
            is KFunction1<*, *> -> ".${it.name}()"
            is Int -> "[$it]"
            else -> ".$it"
        }
}
