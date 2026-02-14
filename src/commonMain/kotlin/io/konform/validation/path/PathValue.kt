package io.konform.validation.path

/** Any value provided by the user, often a field name. */
public data class PathValue(
    val value: Any?,
) : PathSegment {
    override val pathString: String get() = ".$value"

    override fun toString(): String = "ProvidedValue($value)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        return when (other) {
            is PathValue -> other.value == value
            is PathKey -> other.key == value
            else -> false
        }
    }

    override fun hashCode(): Int = value?.hashCode() ?: 0
}
