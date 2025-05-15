package io.konform.validation.types

import io.konform.validation.Invalid
import io.konform.validation.Valid
import io.konform.validation.Validation
import io.konform.validation.ValidationResult
import io.konform.validation.path.ValidationPath

internal class IfNotNullValidation<T : Any>(
    private val validation: Validation<T>,
) : Validation<T?> {
    override fun validate(value: T?): ValidationResult<T?> =
        if (value == null) {
            Valid(value)
        } else {
            // Don't prepend path here since we expect the validation to contain the complete path
            validation(value)
        }
}

public class RequireNotNullValidation<T : Any>(
    private val hint: String,
    private val validation: Validation<T>,
    private val userContext: Any? = null,
) : Validation<T?> {
    override fun validate(value: T?): ValidationResult<T?> =
        if (value == null) {
            Invalid.of(ValidationPath.EMPTY, hint, userContext)
        } else {
            // Don't prepend path here since we expect the validation to contain the complete path
            validation(value)
        }

    internal companion object {
        internal const val DEFAULT_REQUIRED_HINT = "is required"
    }
}
