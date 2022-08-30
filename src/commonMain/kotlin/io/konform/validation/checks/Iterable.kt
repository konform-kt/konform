package io.konform.validation.checks

import io.konform.validation.Builder
import io.konform.validation.ErrorConstructor
import kotlin.jvm.JvmName

@JvmName("minItemsIterable")
fun <T, E> Builder<out Iterable<T>, E>.minItems(
    minSize: Int,
    constructError: ErrorConstructor<Iterable<T>, E>
) = check({ it.count() >= minSize }, constructError)

@JvmName("maxItemsIterable")
fun <T, E> Builder<out Iterable<T>, E>.maxItems(
    maxSize: Int,
    constructError: ErrorConstructor<Iterable<T>, E>
) = check({ it.count() <= maxSize }, constructError)

@JvmName("uniqueItemsIterable")
fun <T, E> Builder<out Iterable<T>, E>.uniqueItems(
    constructError: ErrorConstructor<Iterable<T>, E>
) = check({ it.distinct().count() == it.count() }, constructError)
