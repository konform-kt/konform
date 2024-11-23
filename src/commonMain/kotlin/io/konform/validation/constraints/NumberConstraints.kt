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

@JvmName("maximumLong")
public fun ValidationBuilder<Long>.maximum(maximumInclusive: Int): Constraint<Long> =
    constrain("must be at most '$maximumInclusive'") { it <= maximumInclusive.toLong() }

@JvmName("maximumFloat")
public fun ValidationBuilder<Float>.maximum(maximumInclusive: Int): Constraint<Float> =
    constrain("must be at most '$maximumInclusive'") { it <= maximumInclusive.toFloat() }

@JvmName("maximumDouble")
public fun ValidationBuilder<Double>.maximum(maximumInclusive: Int): Constraint<Double> =
    constrain("must be at most '$maximumInclusive'") { it <= maximumInclusive.toDouble() }

@JvmName("exclusiveMaximumLong")
public fun ValidationBuilder<Long>.exclusiveMaximum(maximumExclusive: Int): Constraint<Long> =
    constrain("must be at most '$maximumExclusive'") { it < maximumExclusive.toLong() }

@JvmName("exclusiveMaximumFloat")
public fun ValidationBuilder<Float>.exclusiveMaximum(maximumExclusive: Int): Constraint<Float> =
    constrain("must be at most '$maximumExclusive'") { it < maximumExclusive.toFloat() }

@JvmName("exclusiveMaximumDouble")
public fun ValidationBuilder<Double>.exclusiveMaximum(maximumExclusive: Int): Constraint<Double> =
    constrain("must be at most '$maximumExclusive'") { it < maximumExclusive.toDouble() }

@JvmName("minimumLong")
public fun ValidationBuilder<Long>.minimum(minimumInclusive: Int): Constraint<Long> =
    constrain("must be at most '$minimumInclusive'") { it >= minimumInclusive.toLong() }

@JvmName("minimumFloat")
public fun ValidationBuilder<Float>.minimum(minimumInclusive: Int): Constraint<Float> =
    constrain("must be at most '$minimumInclusive'") { it >= minimumInclusive.toFloat() }

@JvmName("minimumDouble")
public fun ValidationBuilder<Double>.minimum(minimumInclusive: Int): Constraint<Double> =
    constrain("must be at most '$minimumInclusive'") { it >= minimumInclusive.toDouble() }

@JvmName("exclusiveMinimumLong")
public fun ValidationBuilder<Long>.exclusiveMinimum(minimumExclusive: Int): Constraint<Long> =
    constrain("must be at most '$minimumExclusive'") { it > minimumExclusive.toLong() }

@JvmName("exclusiveMinimumFloat")
public fun ValidationBuilder<Float>.exclusiveMinimum(minimumExclusive: Int): Constraint<Float> =
    constrain("must be at most '$minimumExclusive'") { it > minimumExclusive.toFloat() }

@JvmName("exclusiveMinimumDouble")
public fun ValidationBuilder<Double>.exclusiveMinimum(minimumExclusive: Int): Constraint<Double> =
    constrain("must be at most '$minimumExclusive'") { it > minimumExclusive.toDouble() }
