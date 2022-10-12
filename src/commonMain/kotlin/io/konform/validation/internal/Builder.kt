package io.konform.validation.internal

import io.konform.validation.Constraint
import io.konform.validation.Validation
import io.konform.validation.ValidationBuilder
import io.konform.validation.internal.ValidationBuilderImpl.Companion.PropModifier.NonNull
import io.konform.validation.internal.ValidationBuilderImpl.Companion.PropModifier.Optional
import io.konform.validation.internal.ValidationBuilderImpl.Companion.PropModifier.OptionalRequired
import kotlin.collections.Map.Entry
import kotlin.reflect.KProperty1

internal class ValidationBuilderImpl<C, T> : ValidationBuilder<C, T>() {
    companion object {
        private enum class PropModifier {
            NonNull, Optional, OptionalRequired
        }

        private abstract class PropKey<C, T> {
            abstract fun build(builder: ValidationBuilderImpl<C, *>): Validation<C, T>
        }

        private data class SingleValuePropKey<C, T, R>(
            val property: KProperty1<T, R>,
            val modifier: PropModifier
        ) : PropKey<C, T>() {
            override fun build(builder: ValidationBuilderImpl<C, *>): Validation<C, T> {
                @Suppress("UNCHECKED_CAST")
                val validations = (builder as ValidationBuilderImpl<C, R>).build()
                return when (modifier) {
                    NonNull -> NonNullPropertyValidation(property, validations)
                    Optional -> OptionalPropertyValidation(property, validations)
                    OptionalRequired -> RequiredPropertyValidation(property, validations)
                }
            }
        }

        private data class IterablePropKey<C, T, R>(
            val property: KProperty1<T, Iterable<R>>,
            val modifier: PropModifier
        ) : PropKey<C, T>() {
            override fun build(builder: ValidationBuilderImpl<C, *>): Validation<C, T> {
                @Suppress("UNCHECKED_CAST")
                val validations = (builder as ValidationBuilderImpl<C, R>).build()
                @Suppress("UNCHECKED_CAST")
                return when (modifier) {
                    NonNull -> NonNullPropertyValidation(property, IterableValidation(validations))
                    Optional -> OptionalPropertyValidation(property, IterableValidation(validations))
                    OptionalRequired -> RequiredPropertyValidation(property, IterableValidation(validations))
                }
            }
        }

        private data class ArrayPropKey<C, T, R>(
            val property: KProperty1<T, Array<R>>,
            val modifier: PropModifier
        ) : PropKey<C, T>() {
            override fun build(builder: ValidationBuilderImpl<C, *>): Validation<C, T> {
                @Suppress("UNCHECKED_CAST")
                val validations = (builder as ValidationBuilderImpl<C, R>).build()
                @Suppress("UNCHECKED_CAST")
                return when (modifier) {
                    NonNull -> NonNullPropertyValidation(property, ArrayValidation(validations))
                    Optional -> OptionalPropertyValidation(property, ArrayValidation(validations))
                    OptionalRequired -> RequiredPropertyValidation(property, ArrayValidation(validations))
                }
            }
        }

        private data class MapPropKey<C, T, K, V>(
            val property: KProperty1<T, Map<K, V>>,
            val modifier: PropModifier
        ) : PropKey<C, T>() {
            override fun build(builder: ValidationBuilderImpl<C, *>): Validation<C, T> {
                @Suppress("UNCHECKED_CAST")
                val validations = (builder as ValidationBuilderImpl<C, Entry<K, V>>).build()
                return when (modifier) {
                    NonNull -> NonNullPropertyValidation(property, MapValidation(validations))
                    Optional -> OptionalPropertyValidation(property, MapValidation(validations))
                    OptionalRequired -> RequiredPropertyValidation(property, MapValidation(validations))
                }
            }
        }
    }

    private val constraints = mutableListOf<Constraint<C, T>>()
    private val subValidations = mutableMapOf<PropKey<C, T>, ValidationBuilderImpl<C, *>>()
    private val prebuiltValidations = mutableListOf<Validation<C, T>>()

    override fun Constraint<C, T>.hint(hint: String): Constraint<C, T> =
        Constraint(hint, this.templateValues, this.test).also { constraints.remove(this); constraints.add(it) }

    override fun addConstraint(errorMessage: String, vararg templateValues: String, test: (C, T) -> Boolean): Constraint<C, T> {
        return Constraint(errorMessage, templateValues.toList(), test).also { constraints.add(it) }
    }

    private fun <R> KProperty1<T, R?>.getOrCreateBuilder(modifier: PropModifier): ValidationBuilder<C, R> {
        val key = SingleValuePropKey<C, T, R?>(this, modifier)
        @Suppress("UNCHECKED_CAST")
        return (subValidations.getOrPut(key, { ValidationBuilderImpl<C, R>() }) as ValidationBuilder<C, R>)
    }

    private fun <R> KProperty1<T, Iterable<R>>.getOrCreateIterablePropertyBuilder(modifier: PropModifier): ValidationBuilder<C, R> {
        val key = IterablePropKey<C, T, R>(this, modifier)
        @Suppress("UNCHECKED_CAST")
        return (subValidations.getOrPut(key, { ValidationBuilderImpl<C, R>() }) as ValidationBuilder<C, R>)
    }

    private fun <R> PropKey<C, T>.getOrCreateBuilder(): ValidationBuilder<C, R> {
        @Suppress("UNCHECKED_CAST")
        return (subValidations.getOrPut(this, { ValidationBuilderImpl<C, R>() }) as ValidationBuilder<C, R>)
    }

    override fun <R> KProperty1<T, R>.invoke(init: ValidationBuilder<C, R>.() -> Unit) {
        getOrCreateBuilder(NonNull).also(init)
    }

    override fun <R> onEachIterable(prop: KProperty1<T, Iterable<R>>, init: ValidationBuilder<C, R>.() -> Unit) {
        prop.getOrCreateIterablePropertyBuilder(NonNull).also(init)
    }

    override fun <R> onEachArray(prop: KProperty1<T, Array<R>>, init: ValidationBuilder<C, R>.() -> Unit) {
        ArrayPropKey<C, T, R>(prop, NonNull).getOrCreateBuilder<R>().also(init)
    }

    override fun <K, V> onEachMap(prop: KProperty1<T, Map<K, V>>, init: ValidationBuilder<C, Entry<K, V>>.() -> Unit) {
        MapPropKey<C, T, K, V>(prop, NonNull).getOrCreateBuilder<Entry<K, V>>().also(init)
    }

    override fun <R> KProperty1<T, R?>.ifPresent(init: ValidationBuilder<C, R>.() -> Unit) {
        getOrCreateBuilder(Optional).also(init)
    }

    override fun <R> KProperty1<T, R?>.required(init: ValidationBuilder<C, R>.() -> Unit) {
        getOrCreateBuilder(OptionalRequired).also(init)
    }

    override val <R> KProperty1<T, R>.has: ValidationBuilder<C, R>
        get() = getOrCreateBuilder(NonNull)

    override fun run(validation: Validation<C, T>) {
        prebuiltValidations.add(validation)
    }

    override fun <S> run(validation: Validation<S, T>, map: (C) -> S) {
        prebuiltValidations.add(MappedValidation(validation, map))
    }

    override fun build(): Validation<C, T> {
        val nestedValidations = subValidations.map { (key, builder) ->
            key.build(builder)
        }
        return ValidationNode(constraints, nestedValidations + prebuiltValidations)
    }
}
