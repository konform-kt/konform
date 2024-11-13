package io.konform.validation.constraints

import io.konform.validation.Constraint
import io.konform.validation.ValidationBuilder

public fun <T : Map<*, *>> ValidationBuilder<T>.minItems(minSize: Int): Constraint<T> =
    addConstraint("must have at least {0} items", minSize.toString()) {
        it.count() >= minSize
    }

public fun <T : Map<*, *>> ValidationBuilder<T>.maxItems(maxSize: Int): Constraint<T> =
    addConstraint("must have at most {0} items", maxSize.toString()) {
        it.count() <= maxSize
    }

public fun <T : Map<*, *>> ValidationBuilder<T>.minProperties(minSize: Int): Constraint<T> =
    minItems(minSize) hint "must have at least {0} properties"

public fun <T : Map<*, *>> ValidationBuilder<T>.maxProperties(maxSize: Int): Constraint<T> =
    maxItems(maxSize) hint "must have at most {0} properties"

public fun <T : Map<*, *>> ValidationBuilder<T>.uniqueItems(unique: Boolean = true): Constraint<T> =
    addConstraint("all items must be unique") {
        !unique || it.values.distinct().count() == it.count()
    }
