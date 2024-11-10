package io.konform.validation

import io.konform.validation.path.PathSegment
import io.konform.validation.path.ValidationPath
import io.konform.validation.types.EmptyValidation
import io.konform.validation.types.NullableValidation
import io.konform.validation.types.PrependPathValidation
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

/** Combine a [List] of [Validation]s into a single one that returns all validation errors. */
public fun <T> List<Validation<T>>.flatten(): Validation<T> =
    when (size) {
        0 -> EmptyValidation
        1 -> first()
        else -> ValidateAll(this)
    }

/** Run a validation only if the actual value is not-null. */
public fun <T : Any> Validation<T>.ifPresent(): Validation<T?> = NullableValidation(required = false, validation = this)

/** Require a nullable value to actually be present. */
public fun <T : Any> Validation<T>.required(): Validation<T?> = NullableValidation(required = true, validation = this)
