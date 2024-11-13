package io.konform.validation.types

import io.konform.validation.Constraint
import io.konform.validation.Invalid
import io.konform.validation.Valid
import io.konform.validation.Validation
import io.konform.validation.ValidationError
import io.konform.validation.ValidationResult

public data class ConstraintsValidation<T>(
    private val constraints: List<Constraint<T>>,
) : Validation<T> {
    override fun validate(value: T): ValidationResult<T> =
        constraints
            .filterNot { it.test(value) }
            .map { ValidationError(it.path, it.createHint(value), userContext = it.userContext) }
            .let { errors ->
                if (errors.isEmpty()) {
                    Valid(value)
                } else {
                    Invalid(errors)
                }
            }
}
