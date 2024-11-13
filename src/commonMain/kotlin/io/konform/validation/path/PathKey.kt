package io.konform.validation.path

/**
 * The key of a map or object.
 *
 * Equality: will equal a [PathValue] of the same value
 * */
public data class PathKey(
    val key: Any?,
) : PathSegment {
    override val pathString: String get() = ".$key"

    override fun toString(): String = "PathKey($key)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        return when (other) {
            is PathKey -> other.key == key
            is PathValue -> other.value == key
            else -> false
        }
    }

    override fun hashCode(): Int = key?.hashCode() ?: 0
}

public fun Map.Entry<*, *>.toPathSegment(): PathKey = PathKey(key)
