package io.konform.validation;

import kotlin.reflect.KProperty1

interface ValidationBuilder<T> {
    fun build(): Validation<T>
    fun addConstraint(errorMessage: String, vararg templateValues: String, test: (T) -> Boolean): Constraint<T>
    infix fun <R> Constraint<R>.hint(hint: String): Constraint<R> = this
    operator fun <R> KProperty1<T, R>.invoke(init: ValidationBuilder<R>.() -> Unit)
    infix fun <R> KProperty1<T, R?>.ifPresent(init: ValidationBuilder<R>.() -> Unit)
    infix fun <R> KProperty1<T, R?>.required(init: ValidationBuilder<R>.() -> Unit)
    val <R> KProperty1<T, R>.has: ValidationBuilder<R>
}
