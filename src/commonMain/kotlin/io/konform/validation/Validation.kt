package io.konform.validation

import io.konform.validation.types.CombinedValidations
import io.konform.validation.types.EmptyValidation

public interface Validation<in T> {
    public companion object {
        public operator fun <T> invoke(init: ValidationBuilder<T>.() -> Unit): Validation<T> = ValidationBuilder.buildWithNew(init)
    }

    public fun validate(value: T): ValidationResult<@UnsafeVariance T>

    public operator fun invoke(value: T): ValidationResult<@UnsafeVariance T> = validate(value)
}

/** Combine a [List] of [Validation]s into a single one that returns all validation errors. */
public fun <T> List<Validation<T>>.flatten(): Validation<T> =
    when (size) {
        0 -> EmptyValidation
        1 -> first()
        else -> CombinedValidations(this)
    }
