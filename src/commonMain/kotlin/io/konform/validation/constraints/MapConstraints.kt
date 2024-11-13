package io.konform.validation.constraints

import io.konform.validation.Constraint
import io.konform.validation.ValidationBuilder

public fun <K, V> ValidationBuilder<Map<K, V>>.minItems(minSize: Int): Constraint<Map<K, V>> =
    addConstraint("must have at least {0} items", minSize.toString()) {
        it.count() >= minSize
    }

public fun <K, V> ValidationBuilder<Map<K, V>>.maxItems(maxSize: Int): Constraint<Map<K, V>> =
    addConstraint("must have at most {0} items", maxSize.toString()) {
        it.count() <= maxSize
    }

public fun <K, V> ValidationBuilder<Map<K, V>>.minProperties(minSize: Int): Constraint<Map<K, V>> =
    minItems(minSize) hint "must have at least {0} properties"

public fun <K, V> ValidationBuilder<Map<K, V>>.maxProperties(maxSize: Int): Constraint<Map<K, V>> =
    maxItems(maxSize) hint "must have at most {0} properties"

public fun <K, V> ValidationBuilder<Map<K, V>>.uniqueItems(unique: Boolean = true): Constraint<Map<K, V>> =
    addConstraint("all items must be unique") {
        !unique || it.values.distinct().count() == it.count()
    }
