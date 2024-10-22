package io.konform.validation

import io.konform.validation.ValidationBuilder.PropModifier.NonNull
import io.konform.validation.ValidationBuilder.PropModifier.Optional
import io.konform.validation.ValidationBuilder.PropModifier.OptionalRequired
import io.konform.validation.internal.ArrayValidation
import io.konform.validation.internal.IterableValidation
import io.konform.validation.internal.MapValidation
import io.konform.validation.internal.NonNullPropertyValidation
import io.konform.validation.internal.OptionalPropertyValidation
import io.konform.validation.internal.OptionalValidation
import io.konform.validation.internal.RequiredPropertyValidation
import io.konform.validation.internal.RequiredValidation
import io.konform.validation.internal.ValidationNode
import io.konform.validation.kotlin.Grammar
import kotlin.jvm.JvmName
import kotlin.reflect.KFunction1
import kotlin.reflect.KProperty1

@DslMarker
private annotation class ValidationScope

@ValidationScope
public class ValidationBuilder<T> {
    public fun build(): Validation<T> {
        val nestedValidations =
            subValidations.map { (key, builder) ->
                key.build(builder)
            }
        return ValidationNode(constraints, nestedValidations + prebuiltValidations)
    }

    public fun addConstraint(
        errorMessage: String,
        vararg templateValues: String,
        test: (T) -> Boolean,
    ): Constraint<T> = Constraint(errorMessage, templateValues.toList(), test).also { constraints.add(it) }

    public infix fun Constraint<T>.hint(hint: String): Constraint<T> =
        Constraint(hint, this.templateValues, this.test).also {
            constraints.remove(this)
            constraints.add(it)
        }

    public fun <R> onEachIterable(
        name: String,
        prop: (T) -> Iterable<R>,
        init: ValidationBuilder<R>.() -> Unit,
    ) {
        requireValidName(name)
        init(prop.getOrCreateIterablePropertyBuilder(name, NonNull))
    }

    public fun <R> onEachArray(
        name: String,
        prop: (T) -> Array<R>,
        init: ValidationBuilder<R>.() -> Unit,
    ) {
        requireValidName(name)
        init(ArrayPropKey(prop, name, NonNull).getOrCreateBuilder())
    }

    public fun <K, V> onEachMap(
        name: String,
        prop: (T) -> Map<K, V>,
        init: ValidationBuilder<Map.Entry<K, V>>.() -> Unit,
    ) {
        requireValidName(name)
        init(MapPropKey(prop, name, NonNull).getOrCreateBuilder())
    }

    @JvmName("onEachIterable")
    public infix fun <R> KProperty1<T, Iterable<R>>.onEach(init: ValidationBuilder<R>.() -> Unit): Unit = onEachIterable(name, this, init)

    @JvmName("onEachIterable")
    public infix fun <R> KFunction1<T, Iterable<R>>.onEach(init: ValidationBuilder<R>.() -> Unit): Unit =
        onEachIterable("$name()", this, init)

    @JvmName("onEachArray")
    public infix fun <R> KProperty1<T, Array<R>>.onEach(init: ValidationBuilder<R>.() -> Unit): Unit = onEachArray(name, this, init)

    @JvmName("onEachArray")
    public infix fun <R> KFunction1<T, Array<R>>.onEach(init: ValidationBuilder<R>.() -> Unit): Unit = onEachArray("$name()", this, init)

    @JvmName("onEachMap")
    public infix fun <K, V> KProperty1<T, Map<K, V>>.onEach(init: ValidationBuilder<Map.Entry<K, V>>.() -> Unit): Unit =
        onEachMap(name, this, init)

    @JvmName("onEachMap")
    public infix fun <K, V> KFunction1<T, Map<K, V>>.onEach(init: ValidationBuilder<Map.Entry<K, V>>.() -> Unit): Unit =
        onEachMap("$name()", this, init)

    public operator fun <R> KProperty1<T, R>.invoke(init: ValidationBuilder<R>.() -> Unit): Unit = validate(name, this, init)

    public operator fun <R> KFunction1<T, R>.invoke(init: ValidationBuilder<R>.() -> Unit): Unit = validate("$name()", this, init)

    public infix fun <R> KProperty1<T, R?>.ifPresent(init: ValidationBuilder<R>.() -> Unit): Unit = ifPresent(name, this, init)

    public infix fun <R> KFunction1<T, R?>.ifPresent(init: ValidationBuilder<R>.() -> Unit): Unit = ifPresent("$name()", this, init)

    public infix fun <R> KProperty1<T, R?>.required(init: ValidationBuilder<R>.() -> Unit): Unit = required(name, this, init)

    public infix fun <R> KFunction1<T, R?>.required(init: ValidationBuilder<R>.() -> Unit): Unit = required("$name()", this, init)

    public fun <R> validate(
        name: String,
        f: (T) -> R,
        init: ValidationBuilder<R>.() -> Unit,
    ): Unit = init(f.getOrCreateBuilder(name, NonNull))

    public fun <R> ifPresent(
        name: String,
        f: (T) -> R?,
        init: ValidationBuilder<R>.() -> Unit,
    ): Unit = init(f.getOrCreateBuilder(name, Optional))

    public fun <R> required(
        name: String,
        f: (T) -> R?,
        init: ValidationBuilder<R>.() -> Unit,
    ): Unit = init(f.getOrCreateBuilder(name, OptionalRequired))

    public fun run(validation: Validation<T>) {
        prebuiltValidations.add(validation)
    }

    private fun <R> ((T) -> R?).getOrCreateBuilder(
        name: String,
        modifier: PropModifier,
    ): ValidationBuilder<R> {
        requireValidName(name)
        val key = SingleValuePropKey(this, name, modifier)
        @Suppress("UNCHECKED_CAST")
        return (subValidations.getOrPut(key) { ValidationBuilder<R>() } as ValidationBuilder<R>)
    }

    private fun <R> ((T) -> Iterable<R>).getOrCreateIterablePropertyBuilder(
        name: kotlin.String,
        modifier: PropModifier,
    ): ValidationBuilder<R> {
        val key = IterablePropKey(this, name, modifier)
        @Suppress("UNCHECKED_CAST")
        return (subValidations.getOrPut(key) { ValidationBuilder<R>() } as ValidationBuilder<R>)
    }

    private fun <R> PropKey<T>.getOrCreateBuilder(): ValidationBuilder<R> {
        @Suppress("UNCHECKED_CAST")
        return (subValidations.getOrPut(this) { ValidationBuilder<R>() } as ValidationBuilder<R>)
    }

    private fun requireValidName(name: String) =
        require(Grammar.Identifier.isValid(name) || Grammar.FunctionDeclaration.isUnary(name)) {
            "'$name' is not a valid kotlin identifier or getter name."
        }

    public val <R> KProperty1<T, R>.has: ValidationBuilder<R>
        get() = getOrCreateBuilder(name, NonNull)
    public val <R> KFunction1<T, R>.has: ValidationBuilder<R>
        get() = getOrCreateBuilder(name, NonNull)

    private val constraints = mutableListOf<Constraint<T>>()
    private val subValidations = mutableMapOf<PropKey<T>, ValidationBuilder<*>>()
    private val prebuiltValidations = mutableListOf<Validation<T>>()

    private enum class PropModifier {
        NonNull,
        Optional,
        OptionalRequired,
    }

    private abstract class PropKey<T> {
        abstract fun build(builder: ValidationBuilder<*>): Validation<T>
    }

    private data class SingleValuePropKey<T, R>(
        val property: (T) -> R,
        val name: String,
        val modifier: PropModifier,
    ) : PropKey<T>() {
        override fun build(builder: ValidationBuilder<*>): Validation<T> {
            @Suppress("UNCHECKED_CAST")
            val validations = (builder as ValidationBuilder<R>).build()
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
        override fun build(builder: ValidationBuilder<*>): Validation<T> {
            @Suppress("UNCHECKED_CAST")
            val validations = (builder as ValidationBuilder<R>).build()
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
        override fun build(builder: ValidationBuilder<*>): Validation<T> {
            @Suppress("UNCHECKED_CAST")
            val validations = (builder as ValidationBuilder<R>).build()
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
        override fun build(builder: ValidationBuilder<*>): Validation<T> {
            @Suppress("UNCHECKED_CAST")
            val validations = (builder as ValidationBuilder<Map.Entry<K, V>>).build()
            return when (modifier) {
                NonNull -> NonNullPropertyValidation(property, name, MapValidation(validations))
                Optional -> OptionalPropertyValidation(property, name, MapValidation(validations))
                OptionalRequired -> RequiredPropertyValidation(property, name, MapValidation(validations))
            }
        }
    }
}

/**
 * Run a validation if the property is not-null, and allow nulls.
 */
public fun <T : Any> ValidationBuilder<T?>.ifPresent(init: ValidationBuilder<T>.() -> Unit) {
    val builder = ValidationBuilder<T>()
    init(builder)
    run(OptionalValidation(builder.build()))
}

/**
 * Run a validation on a nullable property, giving an error on nulls.
 */
public fun <T : Any> ValidationBuilder<T?>.required(init: ValidationBuilder<T>.() -> Unit) {
    val builder = ValidationBuilder<T>()
    init(builder)
    run(RequiredValidation(builder.build()))
}

@JvmName("onEachIterable")
public fun <S, T : Iterable<S>> ValidationBuilder<T>.onEach(init: ValidationBuilder<S>.() -> Unit) {
    val builder = ValidationBuilder<S>()
    init(builder)
    @Suppress("UNCHECKED_CAST")
    run(IterableValidation(builder.build()) as Validation<T>)
}

@JvmName("onEachArray")
public fun <T> ValidationBuilder<Array<T>>.onEach(init: ValidationBuilder<T>.() -> Unit) {
    val builder = ValidationBuilder<T>()
    init(builder)
    run(ArrayValidation(builder.build()))
}

@JvmName("onEachMap")
public fun <K, V, T : Map<K, V>> ValidationBuilder<T>.onEach(init: ValidationBuilder<Map.Entry<K, V>>.() -> Unit) {
    val builder = ValidationBuilder<Map.Entry<K, V>>()
    init(builder)
    @Suppress("UNCHECKED_CAST")
    run(MapValidation(builder.build()) as Validation<T>)
}
