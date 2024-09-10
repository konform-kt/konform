package io.konform.validation.internal

import io.konform.validation.Constraint
import io.konform.validation.Validation
import io.konform.validation.ValidationBuilder
import io.konform.validation.internal.ValidationBuilderImpl.Companion.PropModifier.NonNull
import io.konform.validation.internal.ValidationBuilderImpl.Companion.PropModifier.Optional
import io.konform.validation.internal.ValidationBuilderImpl.Companion.PropModifier.OptionalRequired
import io.konform.validation.kotlin.Grammar
import kotlin.collections.Map.Entry
import kotlin.reflect.KFunction1
import kotlin.reflect.KProperty1

internal class ValidationBuilderImpl<T> : ValidationBuilder<T>() {
    companion object {
        private enum class PropModifier {
            NonNull,
            Optional,
            OptionalRequired,
        }

        private abstract class PropKey<T> {
            abstract fun build(builder: ValidationBuilderImpl<*>): Validation<T>
        }

        private data class SingleValuePropKey<T, R>(
            val property: (T) -> R,
            val name: String,
            val modifier: PropModifier,
        ) : PropKey<T>() {
            override fun build(builder: ValidationBuilderImpl<*>): Validation<T> {
                @Suppress("UNCHECKED_CAST")
                val validations = (builder as ValidationBuilderImpl<R>).build()
                return when (modifier) {
                    NonNull -> NonNullPropertyValidation(property, name, validations)
                    Optional -> OptionalPropertyValidation(property, name, validations)
                    OptionalRequired -> RequiredPropertyValidation(property, name, validations)
                }
            }
        }

        private data class IterablePropKey<T, R>(
            val property: (T) -> Iterable<R>,
            val name: String,
            val modifier: PropModifier,
        ) : PropKey<T>() {
            override fun build(builder: ValidationBuilderImpl<*>): Validation<T> {
                @Suppress("UNCHECKED_CAST")
                val validations = (builder as ValidationBuilderImpl<R>).build()
                return when (modifier) {
                    NonNull -> NonNullPropertyValidation(property, name, IterableValidation(validations))
                    Optional -> OptionalPropertyValidation(property, name, IterableValidation(validations))
                    OptionalRequired -> RequiredPropertyValidation(property, name, IterableValidation(validations))
                }
            }
        }

        private data class ArrayPropKey<T, R>(
            val property: (T) -> Array<R>,
            val name: String,
            val modifier: PropModifier,
        ) : PropKey<T>() {
            override fun build(builder: ValidationBuilderImpl<*>): Validation<T> {
                @Suppress("UNCHECKED_CAST")
                val validations = (builder as ValidationBuilderImpl<R>).build()
                return when (modifier) {
                    NonNull -> NonNullPropertyValidation(property, name, ArrayValidation(validations))
                    Optional -> OptionalPropertyValidation(property, name, ArrayValidation(validations))
                    OptionalRequired -> RequiredPropertyValidation(property, name, ArrayValidation(validations))
                }
            }
        }

        private data class MapPropKey<T, K, V>(
            val property: (T) -> Map<K, V>,
            val name: String,
            val modifier: PropModifier,
        ) : PropKey<T>() {
            override fun build(builder: ValidationBuilderImpl<*>): Validation<T> {
                @Suppress("UNCHECKED_CAST")
                val validations = (builder as ValidationBuilderImpl<Map.Entry<K, V>>).build()
                return when (modifier) {
                    NonNull -> NonNullPropertyValidation(property, name, MapValidation(validations))
                    Optional -> OptionalPropertyValidation(property, name, MapValidation(validations))
                    OptionalRequired -> RequiredPropertyValidation(property, name, MapValidation(validations))
                }
            }
        }
    }

    private val constraints = mutableListOf<Constraint<T>>()
    private val subValidations = mutableMapOf<PropKey<T>, ValidationBuilderImpl<*>>()
    private val prebuiltValidations = mutableListOf<Validation<T>>()

    override fun Constraint<T>.hint(hint: String): Constraint<T> =
        Constraint(hint, this.templateValues, this.test).also {
            constraints.remove(this)
            constraints.add(it)
        }

    override fun addConstraint(
        errorMessage: String,
        vararg templateValues: String,
        test: (T) -> Boolean,
    ): Constraint<T> = Constraint(errorMessage, templateValues.toList(), test).also { constraints.add(it) }

    private fun <R> ((T) -> R?).getOrCreateBuilder(
        name: String,
        modifier: PropModifier,
    ): ValidationBuilder<R> {
        requireValidName(name)
        val key = SingleValuePropKey(this, name, modifier)
        @Suppress("UNCHECKED_CAST")
        return (subValidations.getOrPut(key) { ValidationBuilderImpl<R>() } as ValidationBuilder<R>)
    }

    private fun <R> ((T) -> Iterable<R>).getOrCreateIterablePropertyBuilder(
        name: String,
        modifier: PropModifier,
    ): ValidationBuilder<R> {
        val key = IterablePropKey(this, name, modifier)
        @Suppress("UNCHECKED_CAST")
        return (subValidations.getOrPut(key) { ValidationBuilderImpl<R>() } as ValidationBuilder<R>)
    }

    private fun <R> PropKey<T>.getOrCreateBuilder(): ValidationBuilder<R> {
        @Suppress("UNCHECKED_CAST")
        return (subValidations.getOrPut(this) { ValidationBuilderImpl<R>() } as ValidationBuilder<R>)
    }

    override fun <R> onEachIterable(
        name: String,
        prop: (T) -> Iterable<R>,
        init: ValidationBuilder<R>.() -> Unit,
    ) {
        requireValidName(name)
        init(prop.getOrCreateIterablePropertyBuilder(name, NonNull))
    }

    override fun <R> onEachArray(
        name: String,
        prop: (T) -> Array<R>,
        init: ValidationBuilder<R>.() -> Unit,
    ) {
        requireValidName(name)
        init(ArrayPropKey(prop, name, NonNull).getOrCreateBuilder())
    }

    override fun <K, V> onEachMap(
        name: String,
        prop: (T) -> Map<K, V>,
        init: ValidationBuilder<Entry<K, V>>.() -> Unit,
    ) {
        requireValidName(name)
        init(MapPropKey(prop, name, NonNull).getOrCreateBuilder())
    }

    override val <R> KProperty1<T, R>.has: ValidationBuilder<R>
        get() = getOrCreateBuilder(name, NonNull)
    override val <R> KFunction1<T, R>.has: ValidationBuilder<R>
        get() = getOrCreateBuilder(name, NonNull)

    override fun run(validation: Validation<T>) {
        prebuiltValidations.add(validation)
    }

    override fun <R> validate(
        name: String,
        f: (T) -> R,
        init: ValidationBuilder<R>.() -> Unit,
    ) = init(f.getOrCreateBuilder(name, NonNull))

    override fun <R> ifPresent(
        name: String,
        f: (T) -> R?,
        init: ValidationBuilder<R>.() -> Unit,
    ) = init(f.getOrCreateBuilder(name, Optional))

    override fun <R> required(
        name: String,
        f: (T) -> R?,
        init: ValidationBuilder<R>.() -> Unit,
    ) = init(f.getOrCreateBuilder(name, OptionalRequired))

    private fun requireValidName(name: String) =
        require(Grammar.Identifier.isValid(name) || Grammar.FunctionDeclaration.isUnary(name)) {
            "'$name' is not a valid kotlin identifier or getter name."
        }

    override fun build(): Validation<T> {
        val nestedValidations =
            subValidations.map { (key, builder) ->
                key.build(builder)
            }
        return ValidationNode(constraints, nestedValidations + prebuiltValidations)
    }
}
