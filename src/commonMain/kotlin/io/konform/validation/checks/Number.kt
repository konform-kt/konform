package io.konform.validation.checks

import io.konform.validation.Builder
import io.konform.validation.ErrorConstructor
import kotlin.math.roundToInt

fun <T : Number, E> Builder<T, E>.multipleOf(factor: Number, constructError: ErrorConstructor<T, E>) =
    check({
        val factorAsDouble = factor.toDouble()
        val division = it.toDouble() / factorAsDouble
        division.compareTo(division.roundToInt()) == 0
    }, constructError)

fun <T : Number, E> Builder<T, E>.maximum(
    maximumInclusive: Number,
    constructError: ErrorConstructor<T, E>
) = check({ it.toDouble() <= maximumInclusive.toDouble() }, constructError)

fun <T : Number, E> Builder<T, E>.maximumExclusive(
    maximumExclusive: Number,
    constructError: ErrorConstructor<T, E>
) = check({ it.toDouble() < maximumExclusive.toDouble() }, constructError)

fun <T : Number, E> Builder<T, E>.minimum(
    minimumInclusive: Number,
    constructError: ErrorConstructor<T, E>
) = check({ it.toDouble() >= minimumInclusive.toDouble() }, constructError)

fun <T : Number, E> Builder<T, E>.exclusiveMinimum(
    minimumExclusive: Number,
    constructError: ErrorConstructor<T, E>
) = check({ it.toDouble() > minimumExclusive.toDouble() }, constructError)
