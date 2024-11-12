package io.konform.validation.path

import io.konform.validation.helpers.prepend

/** Represents a path to a validation. */
public data class ValidationPath(
    val segments: List<PathSegment>,
) {
    public infix operator fun plus(segment: PathSegment): ValidationPath = ValidationPath(segments + segment)

    public infix operator fun plus(segments: List<PathSegment>): ValidationPath = ValidationPath(this.segments + segments)

    /** A JSONPath-ish representation of the path. */
    public val dataPath: String
        get() = segments.joinToString("") { it.pathString }

    override fun toString(): String = "ValidationPath(${segments.joinToString(", ")})"

    internal fun append(other: ValidationPath): ValidationPath = other.prepend(this)

    internal fun append(pathSegment: PathSegment): ValidationPath = ValidationPath(segments + pathSegment)

    internal fun prepend(other: ValidationPath): ValidationPath =
        when {
            segments.isEmpty() -> other
            other.segments.isEmpty() -> this
            else -> ValidationPath(other.segments + segments)
        }

    internal fun prepend(pathSegment: PathSegment): ValidationPath = ValidationPath(segments.prepend(pathSegment))

    public companion object {
        internal val EMPTY = ValidationPath(emptyList())

        public fun of(pathSegment: PathSegment): ValidationPath = ValidationPath(listOf(pathSegment))

        public fun fromAny(vararg validationPath: Any): ValidationPath =
            ValidationPath(validationPath.map { PathSegment.toPathSegment(it) })
    }
}
