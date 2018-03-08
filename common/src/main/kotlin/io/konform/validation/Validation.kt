package io.konform.validation

import io.konform.validation.internal.ValidationBuilderImpl

interface Validation<T> {

    companion object {
        operator fun <T> invoke(init: ValidationBuilder<T>.() -> Unit): Validation<T> {
            val builder = ValidationBuilderImpl<T>()
            return builder.apply(init).build()
        }
    }

    fun validate(value: T): ValidationResult<T>
    operator fun invoke(value: T) = validate(value)
}


class Constraint<R> internal constructor(val hint: String, val templateValues: List<String>, val test: (R) -> Boolean)
