package io.konform.validation.jsonschema

import io.konform.validation.Constraint
import io.konform.validation.ValidationBuilder
import io.konform.validation.constraints.const as movedConst
import io.konform.validation.constraints.enum as movedEnum
import io.konform.validation.constraints.exclusiveMaximum as movedExclusiveMaximum
import io.konform.validation.constraints.exclusiveMinimum as movedExclusiveMinimum
import io.konform.validation.constraints.maxItems as movedMaxItems
import io.konform.validation.constraints.maxLength as movedMaxLength
import io.konform.validation.constraints.maxProperties as movedMaxProperties
import io.konform.validation.constraints.maximum as movedMaximum
import io.konform.validation.constraints.minItems as movedMinItems
import io.konform.validation.constraints.minLength as movedMinLength
import io.konform.validation.constraints.minProperties as movedMinProperties
import io.konform.validation.constraints.minimum as movedMinimum
import io.konform.validation.constraints.multipleOf as movedMultipleOf
import io.konform.validation.constraints.pattern as movedPattern
import io.konform.validation.constraints.type as movedType
import io.konform.validation.constraints.uniqueItems as movedUniqueItems
import io.konform.validation.constraints.uuid as movedUuid

@Deprecated("Moved to io.konform.validation.constraints", ReplaceWith("type()", "io.konform.validation.constraints.type"))
public inline fun <reified T> ValidationBuilder<*>.type(): Constraint<*> = movedType<T>()

@Deprecated("Moved to io.konform.validation.constraints", ReplaceWith("enum(*allowed)", "io.konform.validation.constraints.enum"))
public fun <T> ValidationBuilder<T>.enum(vararg allowed: T): Constraint<T> = movedEnum(*allowed)

@Deprecated("Moved to io.konform.validation.constraints", ReplaceWith("enum()", "io.konform.validation.constraints.enum"))
public inline fun <reified T : Enum<T>> ValidationBuilder<String>.enum(): Constraint<String> = movedEnum<T>()

@Deprecated("Moved to io.konform.validation.constraints", ReplaceWith("const(expected)", "io.konform.validation.constraints.const"))
public fun <T> ValidationBuilder<T>.const(expected: T): Constraint<T> = movedConst(expected)

@Deprecated(
    "Moved to io.konform.validation.constraints",
    ReplaceWith("minLength(length)", imports = ["io.konform.validation.constraints.minLength"]),
)
public fun ValidationBuilder<String>.minLength(length: Int): Constraint<String> = movedMinLength(length)

@Deprecated(
    "Moved to io.konform.validation.constraints",
    ReplaceWith("maxLength(length)", imports = ["io.konform.validation.constraints.maxLength"]),
)
public fun ValidationBuilder<String>.maxLength(length: Int): Constraint<String> = movedMaxLength(length)

@Deprecated(
    "Moved to io.konform.validation.constraints",
    ReplaceWith("pattern(length)", imports = ["io.konform.validation.constraints.pattern"]),
)
public fun ValidationBuilder<String>.pattern(pattern: String): Constraint<String> = movedPattern(pattern)

@Deprecated(
    "Moved to io.konform.validation.constraints",
    ReplaceWith("uuid()", imports = ["io.konform.validation.constraints.uuid"]),
)
public fun ValidationBuilder<String>.uuid(): Constraint<String> = movedUuid()

@Deprecated(
    "Moved to io.konform.validation.constraints",
    ReplaceWith("pattern(pattern)", imports = ["io.konform.validation.constraints.pattern"]),
)
public fun ValidationBuilder<String>.pattern(pattern: Regex): Constraint<String> = movedPattern(pattern)

@Deprecated(
    "Moved to io.konform.validation.constraints",
    ReplaceWith("multipleOf(factor)", imports = ["io.konform.validation.constraints.multipleOf"]),
)
public fun <T : Number> ValidationBuilder<T>.multipleOf(factor: Number): Constraint<T> = movedMultipleOf(factor)

@Deprecated(
    "Moved to io.konform.validation.constraints",
    ReplaceWith("maximum(maximumInclusive)", "io.konform.validation.constraints.maximum"),
)
public fun <T : Number> ValidationBuilder<T>.maximum(maximumInclusive: Number): Constraint<T> = movedMaximum(maximumInclusive)

@Deprecated(
    "Moved to io.konform.validation.constraints",
    ReplaceWith("exclusiveMaximum(maximumExclusive)", "io.konform.validation.constraints.exclusiveMaximum"),
)
public fun <T : Number> ValidationBuilder<T>.exclusiveMaximum(maximumExclusive: Number): Constraint<T> =
    movedExclusiveMaximum(maximumExclusive)

@Deprecated(
    "Moved to io.konform.validation.constraints",
    ReplaceWith("minimum(minimumInclusive)", "io.konform.validation.constraints.minimum"),
)
public fun <T : Number> ValidationBuilder<T>.minimum(minimumInclusive: Number): Constraint<T> = movedMinimum(minimumInclusive)

@Deprecated(
    "Moved to io.konform.validation.constraints",
    ReplaceWith("exclusiveMinimum(minimumExclusive)", "io.konform.validation.constraints.exclusiveMinimum"),
)
public fun <T : Number> ValidationBuilder<T>.exclusiveMinimum(minimumExclusive: Number): Constraint<T> =
    movedExclusiveMinimum(minimumExclusive)

@Suppress("UNCHECKED_CAST")
@Deprecated(
    "Moved to io.konform.validation.constraints",
    ReplaceWith("minItems(minSize)", "io.konform.validation.constraints.minItems"),
)
public inline fun <reified T> ValidationBuilder<T>.minItems(minSize: Int): Constraint<T> =
    when (T::class) {
        is Iterable<*> -> (this as ValidationBuilder<Iterable<*>>).movedMinItems(minSize) as Constraint<T>
        is Array<*> -> (this as ValidationBuilder<Array<Any?>>).movedMinItems(minSize) as Constraint<T>
        is Map<*, *> -> (this as ValidationBuilder<Map<Any?, *>>).movedMinItems(minSize) as Constraint<T>
        else -> throw IllegalStateException("minItems can not be applied to type ${T::class}")
    }

@Suppress("UNCHECKED_CAST")
@Deprecated(
    "Moved to io.konform.validation.constraints",
    ReplaceWith("maxItems(maxSize)", "io.konform.validation.constraints.maxItems"),
)
public inline fun <reified T> ValidationBuilder<T>.maxItems(maxSize: Int): Constraint<T> =
    when (T::class) {
        is Iterable<*> -> (this as ValidationBuilder<Iterable<*>>).movedMaxItems(maxSize) as Constraint<T>
        is Array<*> -> (this as ValidationBuilder<Array<Any?>>).movedMaxItems(maxSize) as Constraint<T>
        is Map<*, *> -> (this as ValidationBuilder<Map<Any?, *>>).movedMaxItems(maxSize) as Constraint<T>
        else -> throw IllegalStateException("maxItems can not be applied to type ${T::class}")
    }

@Deprecated(
    "Moved to io.konform.validation.constraints",
    ReplaceWith("minProperties(maxSize)", "io.konform.validation.constraints.minProperties"),
)
public fun <K, V> ValidationBuilder<Map<K, V>>.minProperties(minSize: Int): Constraint<Map<K, V>> =
    movedMinProperties(minSize) hint "must have at least {0} properties"

@Deprecated(
    "Moved to io.konform.validation.constraints",
    ReplaceWith("maxProperties(maxSize)", "io.konform.validation.constraints.maxProperties"),
)
public fun <K, V> ValidationBuilder<Map<K, V>>.maxProperties(maxSize: Int): Constraint<Map<K, V>> =
    movedMaxProperties(maxSize) hint "must have at most {0} properties"

@Suppress("UNCHECKED_CAST")
@Deprecated(
    "Moved to io.konform.validation.constraints",
    ReplaceWith("uniqueItems(unique)", "io.konform.validation.constraints.uniqueItems"),
)
public inline fun <reified T> ValidationBuilder<T>.uniqueItems(unique: Boolean): Constraint<T> =
    when (T::class) {
        is Iterable<*> -> (this as ValidationBuilder<Iterable<*>>).movedUniqueItems(unique) as Constraint<T>
        is Array<*> -> (this as ValidationBuilder<Array<Any?>>).movedUniqueItems(unique) as Constraint<T>
        is Map<*, *> -> (this as ValidationBuilder<Map<Any?, *>>).movedUniqueItems(unique) as Constraint<T>
        else -> throw IllegalStateException("uniqueItems can not be applied to type ${T::class}")
    }
