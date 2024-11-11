package io.konform.validation.types

import io.konform.validation.Invalid
import io.konform.validation.Validation
import io.konform.validation.ValidationResult
import io.konform.validation.flattenOrValid

/** Validation that runs multiple validations in sequence. */
public class ValidateAll<T>(
    private val validations: List<Validation<T>>,
) : Validation<T> {
    override fun validate(value: T): ValidationResult<T> {
        val errors = mutableListOf<Invalid>()
        for (validation in validations) {
            val result = validation.validate(value)
            if (result is Invalid) errors += result
        }
        return errors.flattenOrValid(value)
    }

    override fun toString(): String = "ValidateAll(validation=$validations)"
}
