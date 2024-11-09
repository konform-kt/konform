package io.konform.validation.types

import io.konform.validation.Constraint
import io.konform.validation.Invalid
import io.konform.validation.Valid
import io.konform.validation.Validation
import io.konform.validation.ValidationError
import io.konform.validation.ValidationResult
import io.konform.validation.path.ValidationPath

internal class ConstraintsValidation<T>(
    private val path: ValidationPath,
    private val constraints: List<Constraint<T>>,
) : Validation<T> {
    override fun validate(value: T): ValidationResult<T> =
        constraints
            .filterNot { it.test(value) }
            .map { ValidationError(path, it.createHint(value)) }
            .let { errors ->
                if (errors.isEmpty()) {
                    Valid(value)
                } else {
                    Invalid(errors)
                }
            }
}
