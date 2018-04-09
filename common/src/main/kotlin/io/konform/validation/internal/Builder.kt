package io.konform.validation.internal

import io.konform.validation.Constraint
import io.konform.validation.Validation
import io.konform.validation.ValidationBuilder
import io.konform.validation.internal.ValidationBuilderImpl.Companion.PropModifier.*
import kotlin.collections.Map.Entry
import kotlin.reflect.KProperty1

internal class ValidationBuilderImpl<T> : ValidationBuilder<T>() {
    companion object {
        private enum class PropModifier {
            NonNull, Optional, OptionalRequired
        }

        private abstract class PropKey<T> {
            abstract fun build(builder: ValidationBuilderImpl<*>): Validation<T>
        }

        private data class SingleValuePropKey<T, R>(
            val property: KProperty1<T, R>,
            val modifier: PropModifier
        ) : PropKey<T>() {
            override fun build(builder: ValidationBuilderImpl<*>): Validation<T> {
                @Suppress("UNCHECKED_CAST")
                val validations = (builder as ValidationBuilderImpl<R>).build()
                return when (modifier) {
                    NonNull -> NonNullPropertyValidation(property, validations)
                    Optional -> OptionalPropertyValidation(property, validations)
                    OptionalRequired -> RequiredPropertyValidation(property, validations)
                }
            }
        }

        private data class IterablePropKey<T, R>(
            val property: KProperty1<T, Iterable<R>>,
            val modifier: PropModifier
        ) : PropKey<T>() {
            override fun build(builder: ValidationBuilderImpl<*>): Validation<T> {
                @Suppress("UNCHECKED_CAST")
                val validations = (builder as ValidationBuilderImpl<R>).build()
                return when (modifier) {
                    NonNull -> NonNullPropertyValidation(property, IterableValidation(validations))
                    Optional -> OptionalPropertyValidation(property, IterableValidation(validations))
                    OptionalRequired -> RequiredPropertyValidation(property, IterableValidation(validations))
                }
            }
        }

        private data class ArrayPropKey<T, R>(
            val property: KProperty1<T, Array<R>>,
            val modifier: PropModifier
        ) : PropKey<T>() {
            override fun build(builder: ValidationBuilderImpl<*>): Validation<T> {
                @Suppress("UNCHECKED_CAST")
                val validations = (builder as ValidationBuilderImpl<R>).build()
                return when (modifier) {
                    NonNull -> NonNullPropertyValidation(property, ArrayValidation(validations))
                    Optional -> OptionalPropertyValidation(property, ArrayValidation(validations))
                    OptionalRequired -> RequiredPropertyValidation(property, ArrayValidation(validations))
                }
            }
        }

        private data class MapPropKey<T, K, V>(
            val property: KProperty1<T, Map<K, V>>,
            val modifier: PropModifier
        ) : PropKey<T>() {
            override fun build(builder: ValidationBuilderImpl<*>): Validation<T> {
                @Suppress("UNCHECKED_CAST")
                val validations = (builder as ValidationBuilderImpl<Map.Entry<K, V>>).build()
                return when (modifier) {
                    NonNull -> NonNullPropertyValidation(property, MapValidation(validations))
                    Optional -> OptionalPropertyValidation(property, MapValidation(validations))
                    OptionalRequired -> RequiredPropertyValidation(property, MapValidation(validations))
                }
            }
        }


    }

    private val constraints = mutableListOf<Constraint<T>>()
    private val subValidations = mutableMapOf<PropKey<T>, ValidationBuilderImpl<*>>()

    override fun Constraint<T>.hint(hint: String): Constraint<T> =
        Constraint(hint, this.templateValues, this.test).also { constraints.remove(this); constraints.add(it) }

    override fun addConstraint(errorMessage: String, vararg templateValues: String, test: (T) -> Boolean): Constraint<T> {
        return Constraint(errorMessage, templateValues.toList(), test).also { constraints.add(it) }
    }

    private fun <R> KProperty1<T, R?>.getOrCreateBuilder(modifier: PropModifier): ValidationBuilder<R> {
        val key = SingleValuePropKey(this, modifier)
        @Suppress("UNCHECKED_CAST")
        return (subValidations.getOrPut(key, { ValidationBuilderImpl<R>() }) as ValidationBuilder<R>)
    }

    private fun <R> KProperty1<T, Iterable<R>>.getOrCreateIterablePropertyBuilder(modifier: PropModifier): ValidationBuilder<R> {
        val key = IterablePropKey(this, modifier)
        @Suppress("UNCHECKED_CAST")
        return (subValidations.getOrPut(key, { ValidationBuilderImpl<R>() }) as ValidationBuilder<R>)
    }

    private fun <R> PropKey<T>.getOrCreateBuilder(): ValidationBuilder<R> {
        @Suppress("UNCHECKED_CAST")
        return (subValidations.getOrPut(this, { ValidationBuilderImpl<R>() }) as ValidationBuilder<R>)
    }

    override fun <R> KProperty1<T, R>.invoke(init: ValidationBuilder<R>.() -> Unit) {
        getOrCreateBuilder(NonNull).also(init)
    }

    override fun <R> onEachIterable(prop: KProperty1<T, Iterable<R>>, init: ValidationBuilder<R>.() -> Unit) {
        prop.getOrCreateIterablePropertyBuilder(NonNull).also(init)
    }

    override fun <R> onEachArray(prop: KProperty1<T, Array<R>>, init: ValidationBuilder<R>.() -> Unit) {
        ArrayPropKey(prop, NonNull).getOrCreateBuilder<R>().also(init)
    }

    override fun <K, V> onEachMap(prop: KProperty1<T, Map<K, V>>, init: ValidationBuilder<Entry<K, V>>.() -> Unit) {
        MapPropKey(prop, NonNull).getOrCreateBuilder<Map.Entry<K, V>>().also(init)
    }

    override fun <R> KProperty1<T, R?>.ifPresent(init: ValidationBuilder<R>.() -> Unit) {
        getOrCreateBuilder(Optional).also(init)
    }

    override fun <R> KProperty1<T, R?>.required(init: ValidationBuilder<R>.() -> Unit) {
        getOrCreateBuilder(OptionalRequired).also(init)
    }

    override val <R> KProperty1<T, R>.has: ValidationBuilder<R>
        get() = getOrCreateBuilder(NonNull)

    override fun build(): Validation<T> {
        val nestedValidations = subValidations.map { (key, builder) ->
            key.build(builder)
        }
        return ValidationNode(constraints, nestedValidations)
    }
}
