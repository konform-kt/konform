package io.konform.validation

fun <E, T> countFieldsWithErrors(validationResult: ValidationResult<E, T>) = (validationResult as Invalid).internalErrors.size
fun countErrors(validationResult: ValidationResult<*, *>, vararg properties: Any) = validationResult.get(*properties)?.size
    ?: 0
