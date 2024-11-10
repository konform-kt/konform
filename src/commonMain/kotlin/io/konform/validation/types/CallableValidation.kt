package io.konform.validation.types

import io.konform.validation.Invalid
import io.konform.validation.Valid
import io.konform.validation.Validation
import io.konform.validation.ValidationResult
import io.konform.validation.path.PathSegment
import io.konform.validation.path.ValidationPath

/** Validate the result of a property/function. */
public class CallableValidation<T, R>(
    private val path: ValidationPath = ValidationPath.EMPTY,
    private val callable: (T) -> R,
    private val validation: Validation<R>,
) : Validation<T> {
    internal constructor(pathSegment: Any, callable: (T) -> R, validation: Validation<R>) :
        this(ValidationPath.of(PathSegment.toPathSegment(pathSegment)), callable, validation)

    override fun validate(value: T): ValidationResult<T> {
        val toValidate = callable(value)
        return when (val callableResult = validation(toValidate)) {
            is Valid -> Valid(value)
            is Invalid -> callableResult.prependPath(path)
        }
    }

    override fun toString(): String = "CallableValidation(callable=$callable, path=$path, validation=$validation)"
}