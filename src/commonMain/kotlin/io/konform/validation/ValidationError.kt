package io.konform.validation

import io.konform.validation.helpers.prepend
import io.konform.validation.path.PathSegment
import io.konform.validation.path.ValidationPath

/** Represents the path and error of a validation failure. */
public data class ValidationError(
    public val path: ValidationPath,
    public val message: String,
    public val userContext: Any? = null,
) {
    public val dataPath: String get() = path.dataPath

    public inline fun mapPath(f: (List<PathSegment>) -> List<PathSegment>): ValidationError = copy(path = ValidationPath(f(path.segments)))

    public fun prependPath(path: ValidationPath): ValidationError = copy(path = this.path.prepend(path))

    public fun prependPath(pathSegment: PathSegment): ValidationError = mapPath { it.prepend(pathSegment) }

    internal companion object {
        public fun of(
            pathSegment: Any,
            message: String,
            userContext: Any? = null,
        ): ValidationError = ValidationError(ValidationPath.of(pathSegment), message, userContext)

        public fun ofEmptyPath(message: String): ValidationError = ValidationError(ValidationPath.EMPTY, message)
    }
}

public fun List<ValidationError>.filterPath(vararg validationPath: Any): List<ValidationError> {
    val path = ValidationPath.of(*validationPath)
    return filter { it.path == path }
}

public fun List<ValidationError>.filterDataPath(vararg validationPath: Any): List<ValidationError> {
    val dataPath = ValidationPath.of(*validationPath).dataPath
    return filter { it.dataPath == dataPath }
}

public fun List<ValidationError>.messagesAtPath(vararg validationPath: Any): List<String> = filterPath(*validationPath).map { it.message }

public fun List<ValidationError>.messagesAtDataPath(vararg validationPath: Any): List<String> =
    filterDataPath(*validationPath).map { it.message }
