package io.konform.validation.checks

import io.konform.validation.Builder
import io.konform.validation.ErrorConstructor
import kotlin.jvm.JvmName

@JvmName("minItemsArray")
fun <T, E> Builder<Array<T>, E>.minItems(
    minSize: Int,
    constructError: ErrorConstructor<Array<T>, E>
) = check({ it.count() >= minSize }, constructError)

@JvmName("maxItemsArray")
fun <T, E> Builder<Array<T>, E>.maxItems(
    maxSize: Int,
    constructError: ErrorConstructor<Array<T>, E>
) = check({ it.count() <= maxSize }, constructError)

@JvmName("uniqueItemsArray")
fun <T, E> Builder<Array<T>, E>.uniqueItems(
    constructError: ErrorConstructor<Array<T>, E>
) = check({ it.distinct().count() == it.count() }, constructError)
