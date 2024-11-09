package io.konform.validation.types

import io.konform.validation.Invalid
import io.konform.validation.Valid
import io.konform.validation.Validation
import io.konform.validation.ValidationResult
import io.konform.validation.path.PathSegment

/** Validate the result of a property/function. */
internal class CallableValidation<T, R>(
    private val callable: (T) -> R,
    private val path: PathSegment,
    private val validation: Validation<R>,
) : Validation<T> {
    override fun validate(value: T): ValidationResult<T> {
        val toValidate = callable(value)
        return when (val callableResult = validation(toValidate)) {
            is Valid -> Valid(value)
            is Invalid -> callableResult.prependPath(path)
        }
    }
}
