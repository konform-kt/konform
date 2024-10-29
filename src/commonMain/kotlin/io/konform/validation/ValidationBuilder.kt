package io.konform.validation

import io.konform.validation.builder.ArrayPropKey
import io.konform.validation.builder.IterablePropKey
import io.konform.validation.builder.MapPropKey
import io.konform.validation.builder.PropKey
import io.konform.validation.builder.PropModifier
import io.konform.validation.builder.PropModifier.NonNull
import io.konform.validation.builder.PropModifier.Optional
import io.konform.validation.builder.PropModifier.OptionalRequired
import io.konform.validation.builder.SingleValuePropKey
import io.konform.validation.internal.ArrayValidation
import io.konform.validation.internal.IterableValidation
import io.konform.validation.internal.MapValidation
import io.konform.validation.internal.OptionalValidation
import io.konform.validation.internal.RequiredValidation
import io.konform.validation.internal.ValidationNode
import io.konform.validation.kotlin.Grammar
import io.konform.validation.validations.IsClassValidation
import kotlin.jvm.JvmName
import kotlin.reflect.KFunction1
import kotlin.reflect.KProperty1

@DslMarker
private annotation class ValidationScope

@ValidationScope
public class ValidationBuilder<T> {
    private val constraints = mutableListOf<Constraint<T>>()
    private val subValidations = mutableMapOf<PropKey<T>, ValidationBuilder<*>>()
    private val prebuiltValidations = mutableListOf<Validation<T>>()

    public companion object {
        public inline fun <T> buildWithNew(block: ValidationBuilder<T>.() -> Unit): Validation<T> {
            val builder = ValidationBuilder<T>()
            block(builder)
            return builder.build()
        }
    }

    public fun build(): Validation<T> {
        val nestedValidations =
            subValidations.map { (key, builder) ->
                key.build(builder.build())
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

    private fun <R> onEachIterable(
        name: String,
        prop: (T) -> Iterable<R>,
        init: ValidationBuilder<R>.() -> Unit,
    ) {
        requireValidName(name)
        val key = IterablePropKey(prop, name, NonNull)
        init(key.getOrCreateBuilder())
    }

    private fun <R> onEachArray(
        name: String,
        prop: (T) -> Array<R>,
        init: ValidationBuilder<R>.() -> Unit,
    ) {
        requireValidName(name)
        val key = ArrayPropKey(prop, name, NonNull)
        init(key.getOrCreateBuilder())
    }

    private fun <K, V> onEachMap(
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

    /**
     * Calculate a value from the input and run a validation on it.
     * @param name The name that should be reported in validation errors. Must be a valid kotlin name, optionally followed by ().
     * @param f The function for which you want to validate the result of
     * @see run
     */
    public fun <R> validate(
        name: String,
        f: (T) -> R,
        init: ValidationBuilder<R>.() -> Unit,
    ): Unit = init(f.toPropKey(name, NonNull).getOrCreateBuilder())

    /**
     * Calculate a value from the input and run a validation on it, but only if the value is not null.
     */
    public fun <R> ifPresent(
        name: String,
        f: (T) -> R?,
        init: ValidationBuilder<R>.() -> Unit,
    ): Unit = init(f.toPropKey(name, Optional).getOrCreateBuilder())

    /**
     * Calculate a value from the input and run a validation on it, and give an error if the result is null.
     */
    public fun <R> required(
        name: String,
        f: (T) -> R?,
        init: ValidationBuilder<R>.() -> Unit,
    ): Unit = init(f.toPropKey(name, OptionalRequired).getOrCreateBuilder())

    public fun run(validation: Validation<T>) {
        prebuiltValidations.add(validation)
    }

    private fun <R> ((T) -> R?).toPropKey(
        name: String,
        modifier: PropModifier,
    ): PropKey<T> {
        requireValidName(name)
        return SingleValuePropKey(this, name, modifier)
    }

    private fun <R> PropKey<T>.getOrCreateBuilder(): ValidationBuilder<R> {
        @Suppress("UNCHECKED_CAST")
        return subValidations.getOrPut(this) { ValidationBuilder<R>() } as ValidationBuilder<R>
    }

    private fun requireValidName(name: String) =
        require(Grammar.Identifier.isValid(name) || Grammar.FunctionDeclaration.isUnary(name)) {
            "'$name' is not a valid kotlin identifier or getter name."
        }

    public val <R> KProperty1<T, R>.has: ValidationBuilder<R>
        get() = toPropKey(name, NonNull).getOrCreateBuilder()
    public val <R> KFunction1<T, R>.has: ValidationBuilder<R>
        get() = toPropKey(name, NonNull).getOrCreateBuilder()

    public inline fun <reified SubT : T & Any> ifInstanceOf(init: ValidationBuilder<SubT>.() -> Unit): Unit =
        run(IsClassValidation<SubT, T>(SubT::class, required = false, buildWithNew(init)))

    public inline fun <reified SubT : T & Any> requireIsInstanceOf(init: ValidationBuilder<SubT>.() -> Unit): Unit =
        run(IsClassValidation<SubT, T>(SubT::class, required = true, buildWithNew(init)))
}

/**
 * Run a validation if the property is not-null, and allow nulls.
 */
public fun <T : Any> ValidationBuilder<T?>.ifPresent(init: ValidationBuilder<T>.() -> Unit) =
    run(OptionalValidation(ValidationBuilder.buildWithNew(init)))

/**
 * Run a validation on a nullable property, giving an error on nulls.
 */
public fun <T : Any> ValidationBuilder<T?>.required(init: ValidationBuilder<T>.() -> Unit) =
    run(RequiredValidation(ValidationBuilder.buildWithNew(init)))

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
