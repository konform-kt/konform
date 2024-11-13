package io.konform.validation.constraints

import io.konform.validation.Constraint
import io.konform.validation.ValidationBuilder
import kotlin.jvm.JvmName

public fun <T : Iterable<*>> ValidationBuilder<T>.minItems(minSize: Int): Constraint<T> =
    addConstraint("must have at least {0} items", minSize.toString()) { it.count() >= minSize }

@JvmName("arrayMinItems")
public fun <T> ValidationBuilder<Array<T>>.minItems(minSize: Int): Constraint<Array<T>> =
    addConstraint("must have at least {0} items", minSize.toString()) { it.count() >= minSize }

public fun <T : Iterable<*>> ValidationBuilder<T>.maxItems(maxSize: Int): Constraint<T> =
    addConstraint("must have at most {0} items", maxSize.toString()) {
        it.count() <= maxSize
    }

@JvmName("arrayMaxItems")
public fun <T> ValidationBuilder<Array<T>>.maxItems(maxSize: Int): Constraint<Array<T>> =
    addConstraint("must have at most {0} items", maxSize.toString()) {
        it.count() <= maxSize
    }

public fun <T : Iterable<*>> ValidationBuilder<T>.uniqueItems(unique: Boolean = true): Constraint<T> =
    addConstraint("all items must be unique") {
        !unique || it.distinct().count() == it.count()
    }

@JvmName("arrayUniqueItems")
public fun <T> ValidationBuilder<Array<T>>.uniqueItems(unique: Boolean = true): Constraint<Array<T>> =
    addConstraint("all items must be unique") {
        !unique || it.distinct().count() == it.count()
    }
