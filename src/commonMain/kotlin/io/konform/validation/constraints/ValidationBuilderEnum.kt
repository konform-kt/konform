package io.konform.validation.constraints

import io.konform.validation.Constraint
import io.konform.validation.ValidationBuilder

/** Restrict a value to a set of allowed values. */
public fun <T> ValidationBuilder<T>.enum(vararg allowed: T): Constraint<T> {
    val set = allowed.toSet()
    return addConstraint("must be one of: ${set.joinToString("', '", "'", "'")}") {
        it in allowed
    }
}

/** Restrict a [String] to the entry names of an [Enum]. */
public inline fun <reified T : Enum<T>> ValidationBuilder<String>.enum(): Constraint<String> {
    val enumNames = enumValues<T>().mapTo(mutableSetOf()) { it.name }
    return addConstraint("must be one of: ${enumNames.joinToString("', '", "'", "'")}") {
        it in enumNames
    }
}

public fun <T> ValidationBuilder<T>.const(expected: T): Constraint<T> = addConstraint("must be '$expected'") { expected == it }
