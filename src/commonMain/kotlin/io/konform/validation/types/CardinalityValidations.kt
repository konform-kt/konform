package io.konform.validation.types

import io.konform.validation.Valid
import io.konform.validation.Validation
import io.konform.validation.ValidationResult
import io.konform.validation.flattenNonEmpty

/** Validation that always returns [Valid]. `unit` in monadic terms. */
internal object EmptyValidation : Validation<Any?> {
    override fun validate(value: Any?): ValidationResult<Any?> = Valid(value)
}

/** Validation that runs multiple validations in sequence. */
internal class CombinedValidations<T> internal constructor(
    private val validations: List<Validation<T>>,
) : Validation<T> {
    override fun validate(value: T): ValidationResult<T> = validations.map { it.validate(value) }.flattenNonEmpty()
}
