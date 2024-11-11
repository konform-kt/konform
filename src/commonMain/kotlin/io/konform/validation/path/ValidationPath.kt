package io.konform.validation.path

import io.konform.validation.kotlin.Path

/** Represents a path to a validation. */
public data class ValidationPath(
    // val segments: List<PathSegment>,
    val dataPaths: List<String>,
) {
    public companion object {
        public fun fromAny(vararg validationPath: Any): ValidationPath = ValidationPath(validationPath.map { Path.toPath(*validationPath) })
    }
}
