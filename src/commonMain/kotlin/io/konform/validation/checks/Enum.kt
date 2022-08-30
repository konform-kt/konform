package io.konform.validation.checks

import io.konform.validation.Builder
import io.konform.validation.ErrorConstructor

fun <T, E> Builder<T, E>.enum(vararg allowed: T, constructError: ErrorConstructor<T, E>) =
    check({ it in allowed }, constructError)

inline fun <reified T : Enum<T>, E> Builder<String, E>.enum(noinline constructError: ErrorConstructor<String, E>) =
    check({ it in enumValues<T>().map { it.name } }, constructError)
