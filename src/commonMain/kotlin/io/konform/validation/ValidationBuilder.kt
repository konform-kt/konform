package io.konform.validation

import io.konform.validation.ValidationBuilder.Companion.buildWithNew
import io.konform.validation.builder.IterablePropKey
import io.konform.validation.builder.MapPropKey
import io.konform.validation.builder.PropKey
import io.konform.validation.builder.PropModifier
import io.konform.validation.builder.PropModifier.NonNull
import io.konform.validation.builder.PropModifier.Optional
import io.konform.validation.builder.PropModifier.OptionalRequired
import io.konform.validation.builder.SingleValuePropKey
import io.konform.validation.internal.ArrayValidation
import io.konform.validation.internal.MapValidation
import io.konform.validation.kotlin.Grammar
import io.konform.validation.path.PathSegment
import io.konform.validation.path.ValidationPath
import io.konform.validation.types.ConstraintsValidation
import io.konform.validation.types.IsClassValidation
import io.konform.validation.types.IterableValidation
import io.konform.validation.types.NullableValidation
import io.konform.validation.types.CallableValidation
import kotlin.jvm.JvmName
import kotlin.reflect.KFunction1
import kotlin.reflect.KProperty1

@DslMarker
private annotation class ValidationScope

@ValidationScope
public class ValidationBuilder<T> {
    private val constraints = mutableListOf<Constraint<T>>()
    private val subValidations = mutableMapOf<PathSegment, ValidationBuilder<*>>()
    private val prebuiltValidations = mutableListOf<Validation<T>>()

    public fun build(): Validation<T> {
        val validations =
            ArrayList<Validation<T>>(
                (if (constraints.isNotEmpty()) 1 else 0) +
                    prebuiltValidations.size +
                    subValidations.size,
            )
        if (constraints.isNotEmpty()) {
            validations += ConstraintsValidation(ValidationPath.EMPTY, constraints)
        }
        validations +=
            subValidations.map {
                @Suppress("UNCHECKED_CAST")
                (it.value as ValidationBuilder<T>).build()
            }
        validations += prebuiltValidations
        return validations.flatten()
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
        segment: PathSegment,
        prop: (T) -> Array<R>,
        init: ValidationBuilder<R>.() -> Unit,
    ) {
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
     * @param pathSegment The [PathSegment] of the validation
     * @param f The function for which you want to validate the result of
     * @see run
     */
    public fun <R> validate(
        pathSegment: Any,
        f: (T) -> R,
        init: ValidationBuilder<R>.() -> Unit,
    ): Unit = run(
        CallableValidation(
            callable = f,
            path = PathSegment.toPathSegment(pathSegment),
            buildWithNew(init)
        )
    )

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

    public fun run(validation: Validation<T>): Unit {
        prebuiltValidations.add(validation)
    }

    private fun <R> ((T) -> R?).toPropKey(
        name: String,
        modifier: PropModifier,
    ): PropKey<T> {
        requireValidName(name)
        return SingleValuePropKey(this, name, modifier)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <R> PathSegment.getOrCreateBuilder(): ValidationBuilder<R> =
        subValidations.getOrPut(this) { ValidationBuilder<R>() } as ValidationBuilder<R>

    private fun requireValidName(name: String) =
        require(Grammar.Identifier.isValid(name) || Grammar.FunctionDeclaration.isUnary(name)) {
            "'$name' is not a valid kotlin identifier or getter name."
        }

    public val <R> KProperty1<T, R>.has: ValidationBuilder<R>
        get() = PathSegment.Property(this).getOrCreateBuilder()
    public val <R> KFunction1<T, R>.has: ValidationBuilder<R>
        get() = PathSegment.Function(this).getOrCreateBuilder()

    public inline fun <reified SubT : T & Any> ifInstanceOf(init: ValidationBuilder<SubT>.() -> Unit): Unit =
        run(IsClassValidation<SubT, T>(SubT::class, required = false, buildWithNew(init)))

    public inline fun <reified SubT : T & Any> requireInstanceOf(init: ValidationBuilder<SubT>.() -> Unit): Unit =
        run(IsClassValidation<SubT, T>(SubT::class, required = true, buildWithNew(init)))

    public companion object {
        public inline fun <T> buildWithNew(block: ValidationBuilder<T>.() -> Unit): Validation<T> {
            val builder = ValidationBuilder<T>()
            block(builder)
            return builder.build()
        }
    }
}

/**
 * Run a validation if the property is not-null, and allow nulls.
 */
public fun <T : Any> ValidationBuilder<T?>.ifPresent(init: ValidationBuilder<T>.() -> Unit): Unit =
    run(NullableValidation(required = false, validation = buildWithNew(init)))

/**
 * Run a validation on a nullable property, giving an error on nulls.
 */
public fun <T : Any> ValidationBuilder<T?>.required(init: ValidationBuilder<T>.() -> Unit): Unit =
    run(NullableValidation(required = true, validation = buildWithNew(init)))

@JvmName("onEachIterable")
public fun <S, T : Iterable<S>> ValidationBuilder<T>.onEach(init: ValidationBuilder<S>.() -> Unit): Unit =
    run(IterableValidation(buildWithNew(init)))

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
    run(MapValidation(builder.build()))
}
