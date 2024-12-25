package io.konform.validation.types

import io.konform.validation.Invalid
import io.konform.validation.Valid
import io.konform.validation.Validation
import io.konform.validation.ValidationResult

private inline fun <T> gatherInvalid(
    validations: List<Validation<T>>,
    value: T,
): List<Invalid> {
    val errors = mutableListOf<Invalid>()
    for (validation in validations) {
        val result = validation.validate(value)
        if (result is Invalid) errors += result
    }
    return errors
}

public class ValidationAny<T>(
    private val validations: List<Validation<T>>,
    private val createHint: (List<Invalid>) -> String = ::defaultCreateHint,
) : Validation<T> {
    override fun validate(value: T): ValidationResult<T> {
        val invalids = gatherInvalid(validations, value)
        return if (invalids.size < validations.size) {
            Valid(value)
        } else {
            Invalid()
        }
    }

    private companion object {
        private fun defaultCreateHint(invalids: List<Invalid>): String =
            "all validations failed: ${invalids.flatMap { invalid -> invalid.errors.map { it.message } }}"
    }
}
