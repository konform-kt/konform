package io.konform.validation.types

import io.konform.validation.Invalid
import io.konform.validation.Valid
import io.konform.validation.Validation
import io.konform.validation.ValidationResult
import io.konform.validation.flattenNotEmpty
import io.konform.validation.flattenOrValid

public class ValidationAny<T>(
    private val validations: List<Validation<T>>,
    private val aggregateInvalidResults: (List<Invalid>) -> Invalid = List<Invalid>::flattenNotEmpty,
) : Validation<T> {
    override fun validate(value: T): ValidationResult<T> {
        val errors = mutableListOf<Invalid>()
        for (validation in validations) {
            when (val result = validation.validate(value)) {
                // We only need 1 validation to succeed the "any" validation
                is Valid -> return result
                is Invalid -> errors + result
            }
        }
        return errors.flattenOrValid(value)
    }

    override fun toString(): String = "ValidationAny(validation=$validations)"

    private companion object {
        private fun defaultAggregateInvalidResults(invalids: List<Invalid>): Invalid {
            val combinedErrors =
                "all validations failed: ${invalids.flatMap { invalid -> invalid.errors.map { it.message } }}"
        }
    }
}
