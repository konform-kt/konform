package io.konform.validation

import io.konform.validation.path.PathSegment
import io.konform.validation.path.ValidationPath

/** Represents the path and error of a validation failure. */
public data class ValidationError(
    public val path: ValidationPath,
    public val message: String,
) {
    public val dataPath: String get() = path.pathString

    internal fun prependPath(path: ValidationPath) = this.copy(path = this.path.prepend(path))

    internal fun prependPath(pathSegment: PathSegment) = this.copy(path = this.path.prepend(pathSegment))

    internal companion object {
        internal fun of(
            pathSegment: PathSegment,
            message: String,
        ): ValidationError = ValidationError(ValidationPath.of(pathSegment), message)

        internal fun ofAny(
            pathSegment: Any,
            message: String,
        ): ValidationError = of(PathSegment.toPathSegment(pathSegment), message)
    }
}
