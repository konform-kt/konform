package io.konform.validation.types

import io.konform.validation.Validation
import io.konform.validation.ValidationResult
import io.konform.validation.path.ValidationPath

public class PrependPathValidation<T>(
    private val validation: Validation<T>,
    private val path: ValidationPath,
) : Validation<T> {
    override fun validate(value: T): ValidationResult<T> = validation.validate(value).prependPath(path)
}
