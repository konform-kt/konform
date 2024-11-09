package io.konform.validation.types

import io.konform.validation.Invalid
import io.konform.validation.Valid
import io.konform.validation.Validation
import io.konform.validation.ValidationResult
import io.konform.validation.path.PathSegment
import io.konform.validation.path.ValidationPath

internal class NullableValidation<T : Any>(
    private val required: Boolean,
    private val validation: Validation<T>,
    private val pathSegment: PathSegment? = null
) : Validation<T?> {
    override fun validate(value: T?): ValidationResult<T?> =
        if (value == null) {
            if (required) {
                val path = ValidationPath(listOfNotNull(pathSegment))
                Invalid.of(path, "is required")
            } else {
                Valid(value)
            }
        } else {
            // Don't prepend path here since we expect the validation to contain the complete path
            validation(value)
        }
}
