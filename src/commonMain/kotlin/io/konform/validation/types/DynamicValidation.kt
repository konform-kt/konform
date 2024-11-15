package io.konform.validation.types

import io.konform.validation.Invalid
import io.konform.validation.Valid
import io.konform.validation.Validation
import io.konform.validation.ValidationBuilder
import io.konform.validation.ValidationResult
import io.konform.validation.path.ValidationPath

internal class DynamicValidation<T>(
    private val creator: (T) -> Validation<T>,
) : Validation<T> {
    override fun validate(value: T): ValidationResult<T> {
        val validation = creator(value)
        return validation.validate(value)
    }
}

internal class DynamicCallableValidation<T, R>(
    private val path: ValidationPath,
    private val callable: (T) -> R,
    private val builder: ValidationBuilder<R>.(T) -> Unit,
) : Validation<T> {
    override fun validate(value: T): ValidationResult<T> {
        val validation =
            ValidationBuilder<R>()
                .also {
                    builder(it, value)
                }.build()
        val toValidate = callable(value)
        return when (val callableResult = validation(toValidate)) {
            is Valid -> Valid(value)
            is Invalid -> callableResult.prependPath(path)
        }
    }
}
