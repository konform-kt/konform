package io.konform.validation.jsonschema

import io.konform.validation.Constraint
import io.konform.validation.ValidationBuilder
import kotlin.math.roundToInt
import io.konform.validation.string.maxLength as movedMaxLength
import io.konform.validation.string.minLength as movedMinLength
import io.konform.validation.string.pattern as movedPattern
import io.konform.validation.string.uuid as movedUuid

public inline fun <reified T> ValidationBuilder<*>.type(): Constraint<*> =
    addConstraint(
        "must be of the correct type",
    ) { it is T }

public fun <T> ValidationBuilder<T>.enum(vararg allowed: T): Constraint<T> =
    addConstraint(
        "must be one of: {0}",
        allowed.joinToString("', '", "'", "'"),
    ) { it in allowed }

public inline fun <reified T : Enum<T>> ValidationBuilder<String>.enum(): Constraint<String> {
    val enumNames = enumValues<T>().map { it.name }
    return addConstraint(
        "must be one of: {0}",
        enumNames.joinToString("', '", "'", "'"),
    ) { it in enumNames }
}

public fun <T> ValidationBuilder<T>.const(expected: T): Constraint<T> =
    addConstraint(
        "must be {0}",
        expected?.let { "'$it'" } ?: "null",
    ) { expected == it }

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

@Deprecated(
    "Moved to io.konform.validation.string",
    ReplaceWith("minLength(length)", imports = ["io.konform.validation.string.minLength"]),
)
public fun ValidationBuilder<String>.minLength(length: Int): Constraint<String> = movedMinLength(length)

@Deprecated(
    "Moved to io.konform.validation.string",
    ReplaceWith("maxLength(length)", imports = ["io.konform.validation.string.maxLength"]),
)
public fun ValidationBuilder<String>.maxLength(length: Int): Constraint<String> = movedMaxLength(length)

@Deprecated(
    "Moved to io.konform.validation.string",
    ReplaceWith("pattern(length)", imports = ["io.konform.validation.string.pattern"]),
)
public fun ValidationBuilder<String>.pattern(pattern: String): Constraint<String> = movedPattern(pattern)

@Deprecated(
    "Moved to io.konform.validation.string",
    ReplaceWith("uuid()", imports = ["io.konform.validation.string.uuid"]),
)
public fun ValidationBuilder<String>.uuid(): Constraint<String> = movedUuid()

@Deprecated(
    "Moved to io.konform.validation.string",
    ReplaceWith("pattern(pattern)", imports = ["io.konform.validation.string.pattern"]),
)
public fun ValidationBuilder<String>.pattern(pattern: Regex): Constraint<String> = movedPattern(pattern)

public inline fun <reified T> ValidationBuilder<T>.minItems(minSize: Int): Constraint<T> =
    addConstraint(
        "must have at least {0} items",
        minSize.toString(),
    ) {
        when (it) {
            is Iterable<*> -> it.count() >= minSize
            is Array<*> -> it.count() >= minSize
            is Map<*, *> -> it.count() >= minSize
            else -> throw IllegalStateException("minItems can not be applied to type ${T::class}")
        }
    }

public inline fun <reified T> ValidationBuilder<T>.maxItems(maxSize: Int): Constraint<T> =
    addConstraint(
        "must have at most {0} items",
        maxSize.toString(),
    ) {
        when (it) {
            is Iterable<*> -> it.count() <= maxSize
            is Array<*> -> it.count() <= maxSize
            is Map<*, *> -> it.count() <= maxSize
            else -> throw IllegalStateException("maxItems can not be applied to type ${T::class}")
        }
    }

public inline fun <reified T : Map<*, *>> ValidationBuilder<T>.minProperties(minSize: Int): Constraint<T> =
    minItems(minSize) hint "must have at least {0} properties"

public inline fun <reified T : Map<*, *>> ValidationBuilder<T>.maxProperties(maxSize: Int): Constraint<T> =
    maxItems(maxSize) hint "must have at most {0} properties"

public inline fun <reified T> ValidationBuilder<T>.uniqueItems(unique: Boolean): Constraint<T> =
    addConstraint(
        "all items must be unique",
    ) {
        !unique ||
            when (it) {
                is Iterable<*> -> it.distinct().count() == it.count()
                is Array<*> -> it.distinct().count() == it.count()
                else -> throw IllegalStateException("uniqueItems can not be applied to type ${T::class}")
            }
    }
