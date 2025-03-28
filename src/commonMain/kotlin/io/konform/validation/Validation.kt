package io.konform.validation

import io.konform.validation.path.PathSegment
import io.konform.validation.path.ValidationPath
import io.konform.validation.types.EmptyValidation
import io.konform.validation.types.FailFastValidation
import io.konform.validation.types.IfNotNullValidation
import io.konform.validation.types.PrependPathValidation
import io.konform.validation.types.RequireNotNullValidation
import io.konform.validation.types.RequireNotNullValidation.Companion.DEFAULT_REQUIRED_HINT
import io.konform.validation.types.ValidateAll

public interface Validation<in T> {
    public companion object {
        public operator fun <T> invoke(init: ValidationBuilder<T>.() -> Unit): Validation<T> = ValidationBuilder.buildWithNew(init)
    }

    public fun validate(value: T): ValidationResult<@UnsafeVariance T>

    public operator fun invoke(value: T): ValidationResult<@UnsafeVariance T> = validate(value)

    public fun prependPath(path: ValidationPath): Validation<T> = PrependPathValidation(path, this)

    public fun prependPath(pathSegment: PathSegment): Validation<T> = prependPath(ValidationPath.of(pathSegment))
}

/**
 * Combine a [List] of [Validation]s into a single one that returns all validation errors and runs them in sequence.
 * @param failFast if true, stop after the first validation error
 * */
public fun <T> List<Validation<T>>.flatten(failFast: Boolean = false): Validation<T> =
    when (size) {
        0 -> EmptyValidation
        1 -> first()
        else -> if (failFast) FailFastValidation(this) else ValidateAll(this)
    }

/** Run a validation only if the actual value is not-null. */
public fun <T : Any> Validation<T>.ifPresent(): Validation<T?> = IfNotNullValidation(this)

/** Require a nullable value to actually be present. */
public fun <T : Any> Validation<T>.required(hint: String = DEFAULT_REQUIRED_HINT): Validation<T?> = RequireNotNullValidation(hint, this)

/** First validate using this validation, and if the value passes validation run [nextValidation]. */
public infix fun <T> Validation<T>.andThen(nextValidation: Validation<T>): Validation<T> = FailFastValidation(listOf(this, nextValidation))
