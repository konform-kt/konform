package io.konform.validation.constraints

import io.konform.validation.Constraint
import io.konform.validation.ValidationBuilder
import kotlin.math.roundToInt

public fun <T : Number> ValidationBuilder<T>.multipleOf(factor: Number): Constraint<T> {
    val factorAsDouble = factor.toDouble()
    require(factorAsDouble > 0) { "multipleOf requires the factor to be strictly larger than 0" }
    return addConstraint("must be a multiple of '{0}'", factor.toString()) {
        val division = it.toDouble() / factorAsDouble
        division.compareTo(division.roundToInt()) == 0
    }
}

public fun <T : Number> ValidationBuilder<T>.maximum(maximumInclusive: Number): Constraint<T> =
    addConstraint(
        "must be at most '{0}'",
        maximumInclusive.toString(),
    ) { it.toDouble() <= maximumInclusive.toDouble() }

public fun <T : Number> ValidationBuilder<T>.exclusiveMaximum(maximumExclusive: Number): Constraint<T> =
    addConstraint(
        "must be less than '{0}'",
        maximumExclusive.toString(),
    ) { it.toDouble() < maximumExclusive.toDouble() }

public fun <T : Number> ValidationBuilder<T>.minimum(minimumInclusive: Number): Constraint<T> =
    addConstraint(
        "must be at least '{0}'",
        minimumInclusive.toString(),
    ) { it.toDouble() >= minimumInclusive.toDouble() }

public fun <T : Number> ValidationBuilder<T>.exclusiveMinimum(minimumExclusive: Number): Constraint<T> =
    addConstraint(
        "must be greater than '{0}'",
        minimumExclusive.toString(),
    ) { it.toDouble() > minimumExclusive.toDouble() }
