package io.konform.validation.checks

import io.konform.validation.Builder
import io.konform.validation.ErrorConstructor

inline fun <reified T, E> Builder<*, E>.type(noinline constructError: ErrorConstructor<Any?, E>) =
    check({ it is T }, constructError)

fun <T, E> Builder<T, E>.const(expected: T, constructError: ErrorConstructor<T, E>) =
    check({ expected == it }, constructError)
