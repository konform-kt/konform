package io.konform.validation.jsonschema

import io.konform.validation.Constraint
import io.konform.validation.ValidationBuilder
import kotlin.math.roundToInt

inline fun <reified T> ValidationBuilder<*>.type() =
    addConstraint(
        "must be of the correct type"
    ) { it is T }

fun <T> ValidationBuilder<T>.enum(vararg allowed: T) =
    addConstraint(
        "must be one of: {0}",
        allowed.joinToString("', '", "'", "'")
    ) { it in allowed }

inline fun <reified T : Enum<T>> ValidationBuilder<String>.enum(): Constraint<String> {
    val enumNames = enumValues<T>().map { it.name }
    return addConstraint(
        "must be one of: {0}",
        enumNames.joinToString("', '", "'", "'")
    ) { it in enumNames }
}

fun <T> ValidationBuilder<T>.const(expected: T) =
    addConstraint(
        "must be {0}",
        expected?.let { "'$it'" } ?: "null"
    ) { expected == it }


fun <T : Number> ValidationBuilder<T>.multipleOf(factor: Number): Constraint<T> {
    val factorAsDouble = factor.toDouble()
    require(factorAsDouble > 0) { "multipleOf requires the factor to be strictly larger than 0" }
    return addConstraint("must be a multiple of '{0}'", factor.toString()) {
        val division = it.toDouble() / factorAsDouble
        division.compareTo(division.roundToInt()) == 0
    }
}

fun <T: Number> ValidationBuilder<T>.maximum(maximumInclusive: Number) = addConstraint(
    "must be at most '{0}'",
    maximumInclusive.toString()
) { it.toDouble() <= maximumInclusive.toDouble() }

fun <T: Number> ValidationBuilder<T>.exclusiveMaximum(maximumExclusive: Number) = addConstraint(
    "must be less than '{0}'",
    maximumExclusive.toString()
) { it.toDouble() < maximumExclusive.toDouble() }

fun <T: Number> ValidationBuilder<T>.minimum(minimumInclusive: Number) = addConstraint(
    "must be at least '{0}'",
    minimumInclusive.toString()
) { it.toDouble() >= minimumInclusive.toDouble() }

fun <T: Number> ValidationBuilder<T>.exclusiveMinimum(minimumExclusive: Number) = addConstraint(
    "must be greater than '{0}'",
    minimumExclusive.toString()
) { it.toDouble() > minimumExclusive.toDouble() }
