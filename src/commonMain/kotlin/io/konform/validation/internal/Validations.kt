package io.konform.validation.internal

import io.konform.validation.Valid
import io.konform.validation.Validation
import io.konform.validation.ValidationResult
import io.konform.validation.flattenNonEmpty

/** Validation that always returns [Valid]. `unit` in monadic terms. */
internal object EmptyValidation : Validation<Any?> {
    override fun validate(value: Any?): ValidationResult<Any?> = Valid(value)
}

internal class CombinedValidations<T> internal constructor(
    private val validations: List<Validation<T>>,
) : Validation<T> {
    init {
        require(validations.isNotEmpty()) {
            "At least 1 validation required"
        }
    }

    override fun validate(value: T): ValidationResult<T> = validations.map { it.validate(value) }.flattenNonEmpty()
}
