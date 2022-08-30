package io.konform.validation.checks

import io.konform.validation.Builder
import io.konform.validation.ErrorConstructor
import kotlin.jvm.JvmName

@JvmName("minItemsMap")
fun <K, V, E> Builder<Map<K, V>, E>.minItems(
    minSize: Int,
    constructError: ErrorConstructor<Map<K, V>, E>
) = check({ it.count() >= minSize }, constructError)

@JvmName("maxItemsMap")
fun <K, V, E> Builder<Map<K, V>, E>.maxItems(
    maxSize: Int,
    constructError: ErrorConstructor<Map<K, V>, E>
) = check({ it.count() <= maxSize }, constructError)

fun <K, V, E> Builder<Map<K, V>, E>.minProperties(
    minSize: Int,
    constructError: ErrorConstructor<Map<K, V>, E>
) = minItems(minSize, constructError)

fun <K, V, E> Builder<Map<K, V>, E>.maxProperties(
    maxSize: Int,
    constructError: ErrorConstructor<Map<K, V>, E>
) = maxItems(maxSize, constructError)
