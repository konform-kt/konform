package io.konform.validation.types

import io.konform.validation.Invalid
import io.konform.validation.Validation
import io.konform.validation.ValidationResult
import io.konform.validation.flattenOrValid
import io.konform.validation.path.PathSegment

internal class IterableValidation<T>(
    private val validation: Validation<T>,
) : Validation<Iterable<T>> {
    override fun validate(value: Iterable<T>): ValidationResult<Iterable<T>> {
        val errors = mutableListOf<Invalid>()
        value.forEachIndexed { i, element ->
            val result = validation.validate(element)
            if (result is Invalid) {
                errors += result.prependPath(PathSegment.Index(i))
            }
        }
        return errors.flattenOrValid(value)
    }
}

internal class ArrayValidation<T>(
    private val validation: Validation<T>,
) : Validation<Array<T>> {
    override fun validate(value: Array<T>): ValidationResult<Array<T>> {
        val errors = mutableListOf<Invalid>()
        value.forEachIndexed { i, element ->
            val result = validation.validate(element)
            if (result is Invalid) {
                errors += result.prependPath(PathSegment.Index(i))
            }
        }
        return errors.flattenOrValid(value)
    }
}
