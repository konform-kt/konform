package io.konform.validation.internal

import io.konform.validation.Constraint
import io.konform.validation.Validation
import io.konform.validation.ValidationBuilder
import io.konform.validation.internal.ValidationBuilderImpl.Companion.PropModifier.*
import kotlin.reflect.KProperty1

internal class ValidationBuilderImpl<T> : ValidationBuilder<T> {

    companion object {
        private enum class PropModifier {
            NonNull, Optional, OptionalRequired
        }

        private data class PropKey<T, R>(
            val property: KProperty1<T, R>,
            val modifier: PropModifier) {

            fun build(builder: ValidationBuilderImpl<*>): Validation<T> {
                @Suppress("UNCHECKED_CAST")
                val validations = (builder as ValidationBuilderImpl<R>).internalBuild()
                return when (modifier) {
                    NonNull -> PropertyValidation(property, validations)
                    Optional -> OptionalPropertyValidation(property, validations)
                    OptionalRequired -> RequiredPropertyValidation(property, validations)
                }
            }
        }

    }

    private val constraints = mutableListOf<Constraint<T>>()
    private val subValidations = mutableMapOf<PropKey<T, *>, ValidationBuilderImpl<*>>()

    override fun addConstraint(errorMessage: String, vararg templateValues: String, test: (T) -> Boolean): Constraint<T> {
        return Constraint(errorMessage, templateValues.toList(), test).also { constraints.add(it) }
    }

    private fun <R> KProperty1<T, R?>.getOrCreateBuilder(modifier: PropModifier): ValidationBuilder<R> {
        val key = PropKey(this, modifier)
        @Suppress("UNCHECKED_CAST")
        return (subValidations.getOrPut(key, { ValidationBuilderImpl<R>() }) as ValidationBuilder<R>)
    }

    override fun <R> KProperty1<T, R>.invoke(init: ValidationBuilder<R>.() -> Unit) {
        getOrCreateBuilder(NonNull).also(init)
    }

    override fun <R> KProperty1<T, R?>.ifPresent(init: ValidationBuilder<R>.() -> Unit) {
        getOrCreateBuilder(Optional).also(init)
    }

    override fun <R> KProperty1<T, R?>.required(init: ValidationBuilder<R>.() -> Unit) {
        getOrCreateBuilder(OptionalRequired).also(init)
    }

    override val <R> KProperty1<T, R>.has: ValidationBuilder<R>
        get() = getOrCreateBuilder(NonNull)

    private fun internalBuild(): List<Validation<T>> {
        val localValidation = ValueValidation(constraints)
        val nestedValidations = subValidations.map { (key, builder) ->
            key.build(builder)
        }

        return listOf(localValidation) + nestedValidations
    }

    override fun build(): Validation<T> {
        return ClassValidation(internalBuild())
    }
}
