package io.konform.validation.constraints

import io.konform.validation.Constraint
import io.konform.validation.ValidationBuilder

public fun <T : Any> ValidationBuilder<T>.maximum(maximumInclusive: Comparable<T>): Constraint<T> =
    constrain("must be at most '$maximumInclusive'") { maximumInclusive >= it }

public fun <T : Any> ValidationBuilder<T>.exclusiveMaximum(maximumExclusive: Comparable<T>): Constraint<T> =
    constrain("must be less than '$maximumExclusive'") { maximumExclusive > it }

public fun <T : Any> ValidationBuilder<T>.minimum(minimumInclusive: Comparable<T>): Constraint<T> =
    constrain("must be at least '$minimumInclusive'") { minimumInclusive <= it }

public fun <T : Any> ValidationBuilder<T>.exclusiveMinimum(minimumExclusive: Comparable<T>): Constraint<T> =
    constrain("must be greater than '$minimumExclusive'") { minimumExclusive < it }
