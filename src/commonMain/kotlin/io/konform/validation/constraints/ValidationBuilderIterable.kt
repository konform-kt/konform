package io.konform.validation.constraints

import io.konform.validation.Constraint
import io.konform.validation.ValidationBuilder

public fun <T> ValidationBuilder<Iterable<T>>.minItems(minSize: Int): Constraint<Iterable<T>> =
    addConstraint("must have at least {0} items", minSize.toString()) { it.count() >= minSize }

public fun <T> ValidationBuilder<Array<T>>.minItems(minSize: Int): Constraint<Array<T>> =
    addConstraint("must have at least {0} items", minSize.toString()) { it.count() >= minSize }

public fun <T> ValidationBuilder<Iterable<T>>.maxItems(maxSize: Int): Constraint<Iterable<T>> =
    addConstraint("must have at most {0} items", maxSize.toString()) {
        it.count() <= maxSize
    }

public fun <T> ValidationBuilder<Array<T>>.maxItems(maxSize: Int): Constraint<Array<T>> =
    addConstraint("must have at most {0} items", maxSize.toString()) {
        it.count() <= maxSize
    }

public fun <T> ValidationBuilder<Iterable<T>>.uniqueItems(unique: Boolean): Constraint<Iterable<T>> =
    addConstraint("all items must be unique") {
        !unique || it.distinct().count() == it.count()
    }

public fun <T> ValidationBuilder<Array<T>>.uniqueItems(unique: Boolean): Constraint<Array<T>> =
    addConstraint("all items must be unique") {
        !unique || it.distinct().count() == it.count()
    }
