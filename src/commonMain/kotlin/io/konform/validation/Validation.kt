package io.konform.validation

import io.konform.validation.internal.ValidationNodeBuilder
import kotlin.jvm.JvmName

interface Validation<C, T> {

    companion object {
        operator fun <C, T> invoke(init: ValidationBuilder<C, T>.() -> Unit): Validation<C, T> {
            val builder = ValidationNodeBuilder<C, T>()
            return builder.apply(init).build()
        }

        @JvmName("simpleInvoke")
        operator fun <T> invoke(init: ValidationBuilder<Unit, T>.() -> Unit): Validation<Unit, T> {
            val builder = ValidationNodeBuilder<Unit, T>()
            return builder.apply(init).build()
        }
    }

    fun validate(context: C, value: T): ValidationResult<T>
    operator fun invoke(context: C, value: T) = validate(context, value)
}

operator fun <T> Validation<Unit, T>.invoke(value: T) = validate(Unit, value)
