package io.konform.validation.jsonschema

import io.konform.validation.Constraint
import io.konform.validation.ValidationBuilder
import kotlin.jvm.JvmName
import kotlin.math.roundToInt

inline fun <C, reified T> ValidationBuilder<C, *>.type() =
    addConstraint(
        "must be of the correct type"
    ) { it is T }

@JvmName("simpleType")
inline fun <reified T> ValidationBuilder<Unit, *>.type() = type<Unit, T>()

fun <C, T> ValidationBuilder<C, T>.enum(vararg allowed: T) =
    addConstraint(
        "must be one of: {0}",
        allowed.joinToString("', '", "'", "'")
    ) { it in allowed }

inline fun <C, reified T : Enum<T>> ValidationBuilder<C, String>.enum(): Constraint<*, String> {
    val enumNames = enumValues<T>().map { it.name }
    return addConstraint(
        "must be one of: {0}",
        enumNames.joinToString("', '", "'", "'")
    ) { it in enumNames }
}

@JvmName("simpleEnum")
inline fun <reified T : Enum<T>> ValidationBuilder<Unit, String>.enum(): Constraint<*, String> = enum<Unit, T>()

fun <C, T> ValidationBuilder<C, T>.const(expected: T) =
    addConstraint(
        "must be {0}",
        expected?.let { "'$it'" } ?: "null"
    ) { expected == it }


fun <C, T : Number> ValidationBuilder<C, T>.multipleOf(factor: Number): Constraint<C, T> {
    val factorAsDouble = factor.toDouble()
    require(factorAsDouble > 0) { "multipleOf requires the factor to be strictly larger than 0" }
    return addConstraint("must be a multiple of '{0}'", factor.toString()) {
        val division = it.toDouble() / factorAsDouble
        division.compareTo(division.roundToInt()) == 0
    }
}

fun <C, T : Number> ValidationBuilder<C, T>.maximum(maximumInclusive: Number) = addConstraint(
    "must be at most '{0}'",
    maximumInclusive.toString()
) { it.toDouble() <= maximumInclusive.toDouble() }

fun <C, T : Number> ValidationBuilder<C, T>.exclusiveMaximum(maximumExclusive: Number) = addConstraint(
    "must be less than '{0}'",
    maximumExclusive.toString()
) { it.toDouble() < maximumExclusive.toDouble() }

fun <C, T : Number> ValidationBuilder<C, T>.minimum(minimumInclusive: Number) = addConstraint(
    "must be at least '{0}'",
    minimumInclusive.toString()
) { it.toDouble() >= minimumInclusive.toDouble() }

fun <C, T : Number> ValidationBuilder<C, T>.exclusiveMinimum(minimumExclusive: Number) = addConstraint(
    "must be greater than '{0}'",
    minimumExclusive.toString()
) { it.toDouble() > minimumExclusive.toDouble() }

fun <C> ValidationBuilder<C, String>.minLength(length: Int): Constraint<C, String> {
    require(length >= 0) { IllegalArgumentException("minLength requires the length to be >= 0") }
    return addConstraint(
        "must have at least {0} characters",
        length.toString()
    ) { it.length >= length }
}

fun <C> ValidationBuilder<C, String>.maxLength(length: Int): Constraint<*, String> {
    require(length >= 0) { IllegalArgumentException("maxLength requires the length to be >= 0") }
    return addConstraint(
        "must have at most {0} characters",
        length.toString()
    ) { it.length <= length }
}

fun <C> ValidationBuilder<C, String>.pattern(pattern: String) = pattern(pattern.toRegex())

fun <C> ValidationBuilder<C, String>.pattern(pattern: Regex) = addConstraint(
    "must match the expected pattern",
    pattern.toString()
) { it.matches(pattern) }


inline fun <C, reified T> ValidationBuilder<C, T>.minItems(minSize: Int): Constraint<C, T> = addConstraint(
    "must have at least {0} items",
    minSize.toString()
) {
    when (it) {
        is Iterable<*> -> it.count() >= minSize
        is Array<*> -> it.count() >= minSize
        is Map<*, *> -> it.count() >= minSize
        else -> throw IllegalStateException("minItems can not be applied to type ${T::class}")
    }
}


inline fun <C, reified T> ValidationBuilder<C, T>.maxItems(maxSize: Int): Constraint<C, T> = addConstraint(
    "must have at most {0} items",
    maxSize.toString()
) {
    when (it) {
        is Iterable<*> -> it.count() <= maxSize
        is Array<*> -> it.count() <= maxSize
        is Map<*, *> -> it.count() <= maxSize
        else -> throw IllegalStateException("maxItems can not be applied to type ${T::class}")
    }
}

inline fun <C, reified T: Map<*, *>> ValidationBuilder<C, T>.minProperties(minSize: Int): Constraint<C, T> =
    minItems(minSize) hint "must have at least {0} properties"

inline fun <C, reified T: Map<*, *>> ValidationBuilder<C, T>.maxProperties(maxSize: Int): Constraint<C, T> =
    maxItems(maxSize) hint "must have at most {0} properties"

inline fun <C, reified T> ValidationBuilder<C, T>.uniqueItems(unique: Boolean): Constraint<C, T> = addConstraint(
    "all items must be unique"
) {
    !unique || when (it) {
        is Iterable<*> -> it.distinct().count() == it.count()
        is Array<*> -> it.distinct().count() == it.count()
        else -> throw IllegalStateException("uniqueItems can not be applied to type ${T::class}")
    }
}
