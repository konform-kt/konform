package io.konform.validation.errors

import io.konform.validation.Builder

class NumberValidationError(override val message: String) : ValidationError

fun <T : Number> Builder<T, ValidationError>.minimum(
    minimumInclusive: Number
) = check({ it.toDouble() >= minimumInclusive.toDouble() },
    { NumberValidationError("must be at least '$minimumInclusive'") })

fun <T : Number> Builder<T, ValidationError>.maximum(
    maximumInclusive: Number
) = check({ it.toDouble() <= maximumInclusive.toDouble() },
    { NumberValidationError("must be at most '$maximumInclusive'") })
