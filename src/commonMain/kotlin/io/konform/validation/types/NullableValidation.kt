package io.konform.validation.types

import io.konform.validation.Invalid
import io.konform.validation.Valid
import io.konform.validation.Validation
import io.konform.validation.ValidationResult

internal class NullableValidation<T : Any>(
    private val required: Boolean,
    private val validation: Validation<T>,
) : Validation<T?> {
    override fun validate(value: T?): ValidationResult<T?> =
        if (value == null) {
            if (required) {
                Invalid(mapOf("" to listOf("is required")))
            } else {
                Valid(value)
            }
        } else {
            validation(value)
        }
}
