package io.konform.validation

import io.konform.validation.internal.ValidationNodeBuilder
import kotlin.jvm.JvmName

interface Validation<C, T, E> {

    companion object {
        operator fun <C, T, E> invoke(requiredError: E, init: ValidationBuilder<C, T, E>.() -> Unit): Validation<C, T, E> {
            val builder = ValidationNodeBuilder<C, T, E>(requiredError)
            return builder.apply(init).build()
        }

        @JvmName("contextInvoke")
        operator fun <C, T> invoke(init: ValidationBuilder<C, T, String>.() -> Unit): Validation<C, T, String> {
            val builder = ValidationNodeBuilder<C, T, String>("is required")
            return builder.apply(init).build()
        }

        @JvmName("simpleInvoke")
        operator fun <T> invoke(init: ValidationBuilder<Unit, T, String>.() -> Unit): Validation<Unit, T, String> {
            val builder = ValidationNodeBuilder<Unit, T, String>("is required")
            return builder.apply(init).build()
        }
    }

    fun validate(context: C, value: T): ValidationResult<E, T>
    operator fun invoke(context: C, value: T) = validate(context, value)
}

operator fun <T, E> Validation<Unit, T, E>.invoke(value: T) = validate(Unit, value)
