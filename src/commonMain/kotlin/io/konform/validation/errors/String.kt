package io.konform.validation.errors

import io.konform.validation.Builder

class LengthValidationError(override val message: String) : ValidationError

fun Builder<String, ValidationError>.minLength(
    length: Int
) = check({ it.length >= length }, { LengthValidationError("must have at least $length characters") })

fun Builder<String, ValidationError>.maxLength(
    length: Int
) = check({ it.length <= length }, { LengthValidationError("must have at most $length characters") })
