package io.konform.validation.types

import io.konform.validation.Valid
import io.konform.validation.Validation
import io.konform.validation.ValidationResult

/** Validation that always returns [Valid]. `unit` in monadic terms. */
public object EmptyValidation : Validation<Any?> {
    override fun validate(value: Any?): ValidationResult<Any?> = Valid(value)

    override fun toString(): String = "EmptyValidation"

    override fun equals(other: Any?): Boolean = other === this

    override fun hashCode(): Int = 912378
}
