package io.konform.validation.jsonschema

import io.konform.validation.*
import kotlin.jvm.JvmName
import kotlin.math.roundToInt

inline fun <C, reified T, E> ValidationBuilder<C, *, E>.type(noinline hint: HintBuilder<C, Any?, E>) =
    addConstraint(hint, T::class) { it is T }

@JvmName("simpleType")
inline fun <reified T> ValidationBuilder<Unit, *, String>.type() =
    type<Unit, T, String>(stringHintBuilder("must be of the correct type"))

fun <C, T> enumHintBuilder(): HintBuilder<C, T, String> = { _, arguments ->
    @Suppress("UNCHECKED_CAST")
    val names = (arguments[0] as List<String>).joinToString("', '", "'", "'")
    "must be one of: $names"
}

fun <C, T, E> ValidationBuilder<C, T, E>.enum(hint: HintBuilder<C, T, E>, vararg allowed: T) =
    addConstraint(hint, allowed.toList()) { it in allowed }

fun <C, T> ValidationBuilder<C, T, String>.enum(vararg allowed: T): ConstraintBuilder<C, T, String> {
    return enum(enumHintBuilder(), *allowed)
}

// StringHintBuilder("must be one of: {0}")
inline fun <C, reified T : Enum<T>, E> ValidationBuilder<C, String, E>.enum(noinline hint: HintBuilder<C, String, E>) =
    enumValues<T>()
        .map { it.name }
        .let { enumNames -> addConstraint(hint, enumNames) { it in enumNames } }

inline fun <C, reified T : Enum<T>> ValidationBuilder<C, String, String>.enum() =
    enum<C, T, String>(enumHintBuilder())

@JvmName("simpleEnum")
inline fun <reified T : Enum<T>> ValidationBuilder<Unit, String, String>.enum() =
    enum<Unit, T>()

fun <C, T, E> ValidationBuilder<C, T, E>.const(hint: HintBuilder<C, T, E>, expected: T) =
    addConstraint(hint, expected?.let { "'$it'" } ?: "null") { expected == it }

fun <C, T> ValidationBuilder<C, T, String>.const(expected: T) =
    const(stringHintBuilder("must be {0}"), expected)

fun <C, T : Number, E> ValidationBuilder<C, T, E>.multipleOf(hint: HintBuilder<C, T, E>, factor: Number): ConstraintBuilder<C, T, E> {
    val factorAsDouble = factor.toDouble()
    require(factorAsDouble > 0) { "multipleOf requires the factor to be strictly larger than 0" }
    return addConstraint(hint, factor) {
        val division = it.toDouble() / factorAsDouble
        division.compareTo(division.roundToInt()) == 0
    }
}

fun <C, T : Number> ValidationBuilder<C, T, String>.multipleOf(factor: Number): ConstraintBuilder<C, T, String> =
    multipleOf(stringHintBuilder("must be a multiple of '{0}'"), factor)


fun <C, T : Number, E> ValidationBuilder<C, T, E>.maximum(hint: HintBuilder<C, T, E>, maximumInclusive: Number) =
    addConstraint(hint, maximumInclusive) { it.toDouble() <= maximumInclusive.toDouble() }

fun <C, T : Number> ValidationBuilder<C, T, String>.maximum(maximumInclusive: Number) =
    maximum(stringHintBuilder("must be at most '{0}'"), maximumInclusive)

fun <C, T : Number, E> ValidationBuilder<C, T, E>.exclusiveMaximum(hint: HintBuilder<C, T, E>, maximumExclusive: Number) =
    addConstraint(hint, maximumExclusive) { it.toDouble() < maximumExclusive.toDouble() }

fun <C, T : Number> ValidationBuilder<C, T, String>.exclusiveMaximum(maximumExclusive: Number) =
    exclusiveMaximum(stringHintBuilder("must be less than '{0}'"), maximumExclusive)

fun <C, T : Number, E> ValidationBuilder<C, T, E>.minimum(hint: HintBuilder<C, T, E>, minimumInclusive: Number) =
    addConstraint(hint, minimumInclusive) { it.toDouble() >= minimumInclusive.toDouble() }

fun <C, T : Number> ValidationBuilder<C, T, String>.minimum(minimumInclusive: Number) =
    minimum(stringHintBuilder("must be at least '{0}'"), minimumInclusive)

fun <C, T : Number, E> ValidationBuilder<C, T, E>.exclusiveMinimum(hint: HintBuilder<C, T, E>, minimumExclusive: Number) =
    addConstraint(hint, minimumExclusive) { it.toDouble() > minimumExclusive.toDouble() }

fun <C, T : Number> ValidationBuilder<C, T, String>.exclusiveMinimum(minimumExclusive: Number) =
    exclusiveMinimum(stringHintBuilder("must be greater than '{0}'"), minimumExclusive)

fun <C, E> ValidationBuilder<C, String, E>.minLength(hint: HintBuilder<C, String, E>, length: Int): ConstraintBuilder<C, String, E> {
    require(length >= 0) { IllegalArgumentException("minLength requires the length to be >= 0") }
    return addConstraint(hint, length) { it.length >= length }
}
fun <C> ValidationBuilder<C, String, String>.minLength(length: Int) =
    minLength(stringHintBuilder("must have at least {0} characters"), length)

fun <C, E> ValidationBuilder<C, String, E>.maxLength(hint: HintBuilder<C, String, E>, length: Int): ConstraintBuilder<C, String, E> {
    require(length >= 0) { IllegalArgumentException("maxLength requires the length to be >= 0") }
    return addConstraint(hint, length) { it.length <= length }
}

fun <C> ValidationBuilder<C, String, String>.maxLength(length: Int) =
    maxLength(stringHintBuilder("must have at most {0} characters"), length)

fun <C, E> ValidationBuilder<C, String, E>.pattern(hint: HintBuilder<C, String, E>, pattern: Regex) =
    addConstraint(hint, pattern) { it.matches(pattern) }

fun <C> ValidationBuilder<C, String, String>.pattern(pattern: Regex) =
    pattern(stringHintBuilder("must match the expected pattern"), pattern)

fun <C, E> ValidationBuilder<C, String, E>.pattern(hint: HintBuilder<C, String, E>, pattern: String) =
    pattern(hint, pattern.toRegex())

fun <C> ValidationBuilder<C, String, String>.pattern(pattern: String) =
    pattern(pattern.toRegex())

private const val minItemsTemplate = "must have at least {0} items"

@JvmName("minItemsIterable")
fun <C, T : Iterable<*>, E> ValidationBuilder<C, T, E>.minItems(hint: HintBuilder<C, T, E>, minSize: Int) =
    addConstraint(hint, minSize) { it.count() >= minSize }

@JvmName("minItemsIterable")
fun <C, T : Iterable<*>> ValidationBuilder<C, T, String>.minItems(minSize: Int) =
    minItems(stringHintBuilder(minItemsTemplate), minSize)

@JvmName("minItemsArray")
fun <C, T, E> ValidationBuilder<C, Array<T>, E>.minItems(hint: HintBuilder<C, Array<T>, E>, minSize: Int) =
    addConstraint(hint, minSize) { it.count() >= minSize }

@JvmName("minItemsArray")
fun <C, T> ValidationBuilder<C, Array<T>, String>.minItems(minSize: Int) =
    minItems(stringHintBuilder(minItemsTemplate), minSize)

@JvmName("minItemsMap")
fun <C, T : Map<*, *>, E> ValidationBuilder<C, T, E>.minItems(hint: HintBuilder<C, T, E>, minSize: Int) =
    addConstraint(hint, minSize) { it.count() >= minSize }

@JvmName("minItemsMap")
fun <C, T : Map<*, *>> ValidationBuilder<C, T, String>.minItems(minSize: Int) =
    minItems(stringHintBuilder(minItemsTemplate), minSize)


private const val maxItemsTemplate = "must have at most {0} items"

@JvmName("maxItemsIterable")
fun <C, T : Iterable<*>, E> ValidationBuilder<C, T, E>.maxItems(hint: HintBuilder<C, T, E>, maxSize: Int) =
    addConstraint(hint, maxSize) { it.count() <= maxSize }

@JvmName("maxItemsIterable")
fun <C, T : Iterable<*>> ValidationBuilder<C, T, String>.maxItems(maxSize: Int) =
    maxItems(stringHintBuilder(maxItemsTemplate), maxSize)

@JvmName("maxItemsArray")
fun <C, T, E> ValidationBuilder<C, Array<T>, E>.maxItems(hint: HintBuilder<C, Array<T>, E>, maxSize: Int) =
    addConstraint(hint, maxSize) { it.count() <= maxSize }

@JvmName("maxItemsArray")
fun <C, T> ValidationBuilder<C, Array<T>, String>.maxItems(maxSize: Int) =
    maxItems(stringHintBuilder(maxItemsTemplate), maxSize)

@JvmName("maxItemsMap")
fun <C, T : Map<*, *>, E> ValidationBuilder<C, T, E>.maxItems(hint: HintBuilder<C, T, E>, maxSize: Int) =
    addConstraint(hint, maxSize) { it.count() <= maxSize }

@JvmName("maxItemsMap")
fun <C, T : Map<*, *>> ValidationBuilder<C, T, String>.maxItems(maxSize: Int) =
    maxItems(stringHintBuilder(maxItemsTemplate), maxSize)

fun <C, T : Map<*, *>, E> ValidationBuilder<C, T, E>.minProperties(hint: HintBuilder<C, T, E>, minSize: Int) =
    minItems(hint, minSize)

fun <C, T : Map<*, *>> ValidationBuilder<C, T, String>.minProperties(minSize: Int) =
    minProperties(stringHintBuilder("must have at least {0} properties"), minSize)

fun <C, T : Map<*, *>, E> ValidationBuilder<C, T, E>.maxProperties(hint: HintBuilder<C, T, E>, maxSize: Int) =
    maxItems(hint, maxSize)

fun <C, T : Map<*, *>> ValidationBuilder<C, T, String>.maxProperties(maxSize: Int) =
    maxProperties(stringHintBuilder("must have at most {0} properties"), maxSize)


private const val uniqueItemsTemplate = "all items must be unique"

@JvmName("uniqueItemsIterable")
fun <C, T : Iterable<*>, E> ValidationBuilder<C, T, E>.uniqueItems(hint: HintBuilder<C, T, E>, unique: Boolean) =
    addConstraint(hint, unique) { !unique || it.distinct().count() == it.count() }

@JvmName("uniqueItemsIterable")
fun <C, T : Iterable<*>> ValidationBuilder<C, T, String>.uniqueItems(unique: Boolean) =
    uniqueItems(stringHintBuilder(uniqueItemsTemplate), unique)

@JvmName("uniqueItemsArray")
fun <C, T, E> ValidationBuilder<C, Array<T>, E>.uniqueItems(hint: HintBuilder<C, Array<T>, E>, unique: Boolean) =
    addConstraint(hint, unique) { !unique || it.distinct().count() == it.count() }

@JvmName("uniqueItemsArray")
fun <C, T> ValidationBuilder<C, Array<T>, String>.uniqueItems(unique: Boolean) =
    uniqueItems(stringHintBuilder(uniqueItemsTemplate), unique)
