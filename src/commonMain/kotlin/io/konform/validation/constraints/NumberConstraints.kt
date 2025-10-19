package io.konform.validation.constraints

import io.konform.validation.Constraint
import io.konform.validation.ValidationBuilder
import kotlin.jvm.JvmName
import kotlin.math.roundToInt

public fun <T : Number> ValidationBuilder<T>.multipleOf(factor: Number): Constraint<T> {
    val factorAsDouble = factor.toDouble()
    require(factorAsDouble > 0) { "multipleOf requires the factor to be strictly larger than 0" }
    return addConstraint("must be a multiple of '{0}'", factor.toString()) {
        val division = it.toDouble() / factorAsDouble
        division.compareTo(division.roundToInt()) == 0
    }
}

@JvmName("maximumFloat")
public fun ValidationBuilder<Float>.maximum(maximumInclusive: Int): Constraint<Float> =
    constrain("must be at most '$maximumInclusive'") { it <= maximumInclusive.toFloat() }

@JvmName("maximumDouble")
public fun ValidationBuilder<Double>.maximum(maximumInclusive: Int): Constraint<Double> =
    constrain("must be at most '$maximumInclusive'") { it <= maximumInclusive.toDouble() }

@JvmName("exclusiveMaximumFloat")
public fun ValidationBuilder<Float>.exclusiveMaximum(maximumExclusive: Int): Constraint<Float> =
    constrain("must be less than '$maximumExclusive'") { it < maximumExclusive.toFloat() }

@JvmName("exclusiveMaximumDouble")
public fun ValidationBuilder<Double>.exclusiveMaximum(maximumExclusive: Int): Constraint<Double> =
    constrain("must be less than '$maximumExclusive'") { it < maximumExclusive.toDouble() }

@JvmName("minimumFloat")
public fun ValidationBuilder<Float>.minimum(minimumInclusive: Int): Constraint<Float> =
    constrain("must be at least '$minimumInclusive'") { it >= minimumInclusive.toFloat() }

@JvmName("minimumDouble")
public fun ValidationBuilder<Double>.minimum(minimumInclusive: Int): Constraint<Double> =
    constrain("must be at least '$minimumInclusive'") { it >= minimumInclusive.toDouble() }

@JvmName("exclusiveMinimumFloat")
public fun ValidationBuilder<Float>.exclusiveMinimum(minimumExclusive: Int): Constraint<Float> =
    constrain("must be greater than '$minimumExclusive'") { it > minimumExclusive.toFloat() }

@JvmName("exclusiveMinimumDouble")
public fun ValidationBuilder<Double>.exclusiveMinimum(minimumExclusive: Int): Constraint<Double> =
    constrain("must be greater than '$minimumExclusive'") { it > minimumExclusive.toDouble() }
