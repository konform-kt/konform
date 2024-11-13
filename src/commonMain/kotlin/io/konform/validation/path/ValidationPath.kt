package io.konform.validation.path

import io.konform.validation.helpers.prepend

/** Represents a path to a validation. */
public data class ValidationPath(
    val segments: List<PathSegment>,
) {
    /** A JSONPath-ish representation of the path. */
    public val dataPath: String
        get() = segments.joinToString("") { it.pathString }

    public fun prepend(other: ValidationPath): ValidationPath =
        when {
            segments.isEmpty() -> other
            other.segments.isEmpty() -> this
            else -> ValidationPath(other.segments + segments)
        }

    public fun prepend(pathSegment: PathSegment): ValidationPath = ValidationPath(segments.prepend(pathSegment))

    public infix operator fun plus(segment: PathSegment): ValidationPath = ValidationPath(segments + segment)

    public infix operator fun plus(other: ValidationPath): ValidationPath = other.prepend(this)

    override fun toString(): String = "ValidationPath(${segments.joinToString(", ")})"

    public companion object {
        internal val EMPTY = ValidationPath(emptyList())

        public fun of(vararg validationPath: Any): ValidationPath = ValidationPath(validationPath.map { PathSegment.toPathSegment(it) })
    }
}
