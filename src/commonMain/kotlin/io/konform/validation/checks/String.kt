package io.konform.validation.checks

import io.konform.validation.Builder
import io.konform.validation.ErrorConstructor

fun <E> Builder<String, E>.minLength(
    length: Int,
    constructError: ErrorConstructor<String, E>
) = check({ it.length >= length }, constructError)

fun <E> Builder<String, E>.empty(constructError: ErrorConstructor<String, E>) =
    maxLength(0, constructError)

fun <E> Builder<String, E>.notEmpty(constructError: ErrorConstructor<String, E>) =
    minLength(1, constructError)

fun <E> Builder<String, E>.maxLength(
    length: Int,
    constructError: ErrorConstructor<String, E>
) = check({ it.length <= length }, constructError)

fun <E> Builder<String, E>.pattern(pattern: String, constructError: ErrorConstructor<String, E>) =
    pattern(pattern.toRegex(), constructError)

fun <E> Builder<String, E>.pattern(pattern: Regex, constructError: ErrorConstructor<String, E>) =
    check({ it.matches(pattern) }, constructError)
