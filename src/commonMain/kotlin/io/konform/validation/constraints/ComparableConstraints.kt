package io.konform.validation.constraints

import io.konform.validation.Constraint
import io.konform.validation.ValidationBuilder

public fun <T : Comparable<T>> ValidationBuilder<T>.maximum(maximumInclusive: T): Constraint<T> =
    constrain("must be at most '$maximumInclusive'") { maximumInclusive >= it }

public fun <T : Comparable<T>> ValidationBuilder<T>.exclusiveMaximum(maximumExclusive: T): Constraint<T> =
    constrain("must be less than '$maximumExclusive'") { maximumExclusive > it }

public fun <T : Comparable<T>> ValidationBuilder<T>.minimum(minimumInclusive: T): Constraint<T> =
    constrain("must be at least '$minimumInclusive'") { minimumInclusive <= it }

public fun <T : Comparable<T>> ValidationBuilder<T>.exclusiveMinimum(minimumExclusive: T): Constraint<T> =
    constrain("must be greater than '$minimumExclusive'") { minimumExclusive < it }
