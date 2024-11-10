package io.konform.validation.types

import io.konform.validation.Validation
import io.konform.validation.ValidationResult
import io.konform.validation.path.ValidationPath

public class PrependPathValidation<T>(
    private val path: ValidationPath,
    private val validation: Validation<T>,
) : Validation<T> {
    override fun validate(value: T): ValidationResult<T> = validation.validate(value).prependPath(path)

    override fun toString(): String = "PrependPathValidation(path=$path,validation=$validation)"
}
