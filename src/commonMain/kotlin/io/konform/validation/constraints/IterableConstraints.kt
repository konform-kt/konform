package io.konform.validation.constraints

import io.konform.validation.Constraint
import io.konform.validation.ValidationBuilder
import kotlin.jvm.JvmName

public fun <T : Iterable<*>> ValidationBuilder<T>.minItems(minSize: Int): Constraint<T> =
    constrain("must have at least $minSize items") { it.count() >= minSize }

@JvmName("arrayMinItems")
public fun <T> ValidationBuilder<Array<T>>.minItems(minSize: Int): Constraint<Array<T>> =
    constrain("must have at least $minSize items") { it.count() >= minSize }

public fun <T : Iterable<*>> ValidationBuilder<T>.maxItems(maxSize: Int): Constraint<T> =
    constrain("must have at most $maxSize items") { it.count() <= maxSize }

@JvmName("arrayMaxItems")
public fun <T> ValidationBuilder<Array<T>>.maxItems(maxSize: Int): Constraint<Array<T>> =
    constrain("must have at most $maxSize items") { it.count() <= maxSize }

public fun <T : Iterable<*>> ValidationBuilder<T>.uniqueItems(unique: Boolean = true): Constraint<T> =
    constrain("all items must be unique") { !unique || it.distinct().count() == it.count() }

@JvmName("arrayUniqueItems")
public fun <T> ValidationBuilder<Array<T>>.uniqueItems(unique: Boolean = true): Constraint<Array<T>> =
    constrain("all items must be unique") { !unique || it.distinct().count() == it.count() }
