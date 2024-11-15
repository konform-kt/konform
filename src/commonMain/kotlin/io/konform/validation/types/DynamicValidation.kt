package io.konform.validation.types

import io.konform.validation.Validation
import io.konform.validation.ValidationResult

internal class DynamicValidation<T>(
    private val creator: (T) -> Validation<T>,
) : Validation<T> {
    override fun validate(value: T): ValidationResult<T> {
        val validation = creator(value)
        return validation.validate(value)
    }
}
