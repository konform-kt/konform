package io.konform.validation.path

/** An index to an array, list, or other iterable. */
public data class PathIndex(
    val index: Int,
) : PathSegment {
    override val pathString: String get() = "[$index]"

    override fun toString(): String = "PathIndex($index)"
}
