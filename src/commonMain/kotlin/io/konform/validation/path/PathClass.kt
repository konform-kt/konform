package io.konform.validation.path

import kotlin.reflect.KClass

/** A path for a class reference. */
public data class PathClass(
    val kcls: KClass<*>,
) : PathSegment {
    private val name get() = kcls.simpleName ?: "Anonymous"

    override val pathString: String get() = name

    override fun toString(): String = "PathClass($name)"
}
