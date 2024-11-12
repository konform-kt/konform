package io.konform.validation

import io.konform.validation.ValidationBuilder.Companion.buildWithNew
import io.konform.validation.helpers.prepend
import io.konform.validation.path.PathSegment
import io.konform.validation.path.PathSegment.Companion.toPathSegment
import io.konform.validation.path.ValidationPath
import io.konform.validation.types.ArrayValidation
import io.konform.validation.types.CallableValidation
import io.konform.validation.types.ConstraintsValidation
import io.konform.validation.types.IsClassValidation
import io.konform.validation.types.IterableValidation
import io.konform.validation.types.MapValidation
import kotlin.jvm.JvmName
import kotlin.reflect.KFunction1
import kotlin.reflect.KProperty1

@DslMarker
private annotation class ValidationScope

@ValidationScope
public class ValidationBuilder<T> {
    private val constraints = mutableListOf<Constraint<T>>()
    private val subValidations = mutableListOf<Validation<T>>()

    public fun build(): Validation<T> =
        subValidations
            .let {
                if (constraints.isNotEmpty()) {
                    it.prepend(ConstraintsValidation(ValidationPath.EMPTY, constraints))
                } else {
                    it
                }
            }.flatten()

    public fun addConstraint(
        errorMessage: String,
        vararg templateValues: String,
        test: (T) -> Boolean,
    ): Constraint<T> = Constraint(errorMessage, templateValues.toList(), test).also { constraints.add(it) }

    @Suppress("DEPRECATION")
    public infix fun Constraint<T>.hint(hint: String): Constraint<T> =
        Constraint(hint, this.templateValues, this.test).also {
            constraints.remove(this)
            constraints.add(it)
        }

    private fun <R> onEachIterable(
        pathSegment: PathSegment,
        prop: (T) -> Iterable<R>,
        init: ValidationBuilder<R>.() -> Unit,
    ) = run(CallableValidation(pathSegment, prop, IterableValidation(buildWithNew(init))))

    private fun <R> onEachArray(
        pathSegment: PathSegment,
        prop: (T) -> Array<R>,
        init: ValidationBuilder<R>.() -> Unit,
    ) = run(CallableValidation(pathSegment, prop, ArrayValidation(buildWithNew(init))))

    private fun <K, V> onEachMap(
        pathSegment: PathSegment,
        prop: (T) -> Map<K, V>,
        init: ValidationBuilder<Map.Entry<K, V>>.() -> Unit,
    ) = run(CallableValidation(pathSegment, prop, MapValidation(buildWithNew(init))))

    @JvmName("onEachIterable")
    public infix fun <R> KProperty1<T, Iterable<R>>.onEach(init: ValidationBuilder<R>.() -> Unit): Unit =
        onEachIterable(PathSegment.Prop(this), this, init)

    @JvmName("onEachIterable")
    public infix fun <R> KFunction1<T, Iterable<R>>.onEach(init: ValidationBuilder<R>.() -> Unit): Unit =
        onEachIterable(PathSegment.Func(this), this, init)

    @JvmName("onEachArray")
    public infix fun <R> KProperty1<T, Array<R>>.onEach(init: ValidationBuilder<R>.() -> Unit): Unit =
        onEachArray(PathSegment.Prop(this), this, init)

    @JvmName("onEachArray")
    public infix fun <R> KFunction1<T, Array<R>>.onEach(init: ValidationBuilder<R>.() -> Unit): Unit =
        onEachArray(PathSegment.Func(this), this, init)

    @JvmName("onEachMap")
    public infix fun <K, V> KProperty1<T, Map<K, V>>.onEach(init: ValidationBuilder<Map.Entry<K, V>>.() -> Unit): Unit =
        onEachMap(PathSegment.Prop(this), this, init)

    @JvmName("onEachMap")
    public infix fun <K, V> KFunction1<T, Map<K, V>>.onEach(init: ValidationBuilder<Map.Entry<K, V>>.() -> Unit): Unit =
        onEachMap(PathSegment.Func(this), this, init)

    public operator fun <R> KProperty1<T, R>.invoke(init: ValidationBuilder<R>.() -> Unit): Unit =
        validate(PathSegment.Prop(this), this, init)

    public operator fun <R> KFunction1<T, R>.invoke(init: ValidationBuilder<R>.() -> Unit): Unit = validate(this, this, init)

    public infix fun <R> KProperty1<T, R?>.ifPresent(init: ValidationBuilder<R>.() -> Unit): Unit = ifPresent(this, this, init)

    public infix fun <R> KFunction1<T, R?>.ifPresent(init: ValidationBuilder<R>.() -> Unit): Unit = ifPresent(this, this, init)

    public infix fun <R> KProperty1<T, R?>.required(init: ValidationBuilder<R>.() -> Unit): Unit = required(this, this, init)

    public infix fun <R> KFunction1<T, R?>.required(init: ValidationBuilder<R>.() -> Unit): Unit = required(this, this, init)

    /**
     * Calculate a value from the input and run a validation on it.
     * @param pathSegment The [PathSegment] of the validation.
     *   is [Any] for backwards compatibility and easy of use, see [toPathSegment]
     * @param f The function for which you want to validate the result of
     */
    public fun <R> validate(
        pathSegment: Any,
        f: (T) -> R,
        init: ValidationBuilder<R>.() -> Unit,
    ): Unit = run(CallableValidation(pathSegment, f, buildWithNew(init)))

    /**
     * Calculate a value from the input and run a validation on it, but only if the value is not null.
     */
    public fun <R> ifPresent(
        pathSegment: Any,
        f: (T) -> R?,
        init: ValidationBuilder<R>.() -> Unit,
    ): Unit = run(CallableValidation(pathSegment, f, buildWithNew(init).ifPresent()))

    /**
     * Calculate a value from the input and run a validation on it, and give an error if the result is null.
     */
    public fun <R> required(
        pathSegment: Any,
        f: (T) -> R?,
        init: ValidationBuilder<R>.() -> Unit,
    ): Unit = run(CallableValidation(pathSegment, f, buildWithNew(init).required()))

    public fun run(validation: Validation<T>) {
        subValidations.add(validation)
    }

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
public fun <T : Any> ValidationBuilder<T?>.ifPresent(init: ValidationBuilder<T>.() -> Unit): Unit = run(buildWithNew(init).ifPresent())

/**
 * Run a validation on a nullable property, giving an error on nulls.
 */
public fun <T : Any> ValidationBuilder<T?>.required(init: ValidationBuilder<T>.() -> Unit): Unit = run(buildWithNew(init).required())

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
