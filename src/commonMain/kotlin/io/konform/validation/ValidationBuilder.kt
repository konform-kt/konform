package io.konform.validation

import io.konform.validation.internal.ArrayValidation
import io.konform.validation.internal.IterableValidation
import io.konform.validation.internal.MapValidation
import io.konform.validation.internal.OptionalValidation
import io.konform.validation.internal.RequiredValidation
import io.konform.validation.internal.ValidationBuilderImpl
import kotlin.jvm.JvmName
import kotlin.reflect.KFunction1
import kotlin.reflect.KProperty1

@DslMarker
private annotation class ValidationScope

@ValidationScope
public abstract class ValidationBuilder<T> {
    public abstract fun build(): Validation<T>

    public abstract fun addConstraint(
        errorMessage: String,
        vararg templateValues: String,
        test: (T) -> Boolean,
    ): Constraint<T>

    public abstract infix fun Constraint<T>.hint(hint: String): Constraint<T>

    public abstract operator fun <R> ((T) -> R).invoke(
        name: String,
        init: ValidationBuilder<R>.() -> Unit,
    )

    internal abstract fun <R> onEachIterable(
        name: String,
        prop: (T) -> Iterable<R>,
        init: ValidationBuilder<R>.() -> Unit,
    )

    internal abstract fun <R> onEachArray(
        name: String,
        prop: (T) -> Array<R>,
        init: ValidationBuilder<R>.() -> Unit,
    )

    internal abstract fun <K, V> onEachMap(
        name: String,
        prop: (T) -> Map<K, V>,
        init: ValidationBuilder<Map.Entry<K, V>>.() -> Unit,
    )

    public abstract fun <R> ((T) -> R?).ifPresent(
        name: String,
        init: ValidationBuilder<R>.() -> Unit,
    )

    public abstract fun <R> ((T) -> R?).required(
        name: String,
        init: ValidationBuilder<R>.() -> Unit,
    )

    public operator fun <R> KProperty1<T, R>.invoke(init: ValidationBuilder<R>.() -> Unit): Unit = invoke(name, init)

    public operator fun <R> KFunction1<T, R>.invoke(init: ValidationBuilder<R>.() -> Unit): Unit = invoke("$name()", init)

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

    public infix fun <R> KProperty1<T, R?>.ifPresent(init: ValidationBuilder<R>.() -> Unit): Unit = ifPresent(name, init)

    public infix fun <R> KFunction1<T, R?>.ifPresent(init: ValidationBuilder<R>.() -> Unit): Unit = ifPresent("$name()", init)

    public infix fun <R> KProperty1<T, R?>.required(init: ValidationBuilder<R>.() -> Unit): Unit = required(name, init)

    public infix fun <R> KFunction1<T, R?>.required(init: ValidationBuilder<R>.() -> Unit): Unit = required("$name()", init)

    public abstract fun run(validation: Validation<T>)

    public abstract val <R> KProperty1<T, R>.has: ValidationBuilder<R>
    public abstract val <R> KFunction1<T, R>.has: ValidationBuilder<R>
}

public fun <T : Any> ValidationBuilder<T?>.ifPresent(init: ValidationBuilder<T>.() -> Unit) {
    val builder = ValidationBuilderImpl<T>()
    init(builder)
    run(OptionalValidation(builder.build()))
}

public fun <T : Any> ValidationBuilder<T?>.required(init: ValidationBuilder<T>.() -> Unit) {
    val builder = ValidationBuilderImpl<T>()
    init(builder)
    run(RequiredValidation(builder.build()))
}

@JvmName("onEachIterable")
public fun <S, T : Iterable<S>> ValidationBuilder<T>.onEach(init: ValidationBuilder<S>.() -> Unit) {
    val builder = ValidationBuilderImpl<S>()
    init(builder)
    @Suppress("UNCHECKED_CAST")
    run(IterableValidation(builder.build()) as Validation<T>)
}

@JvmName("onEachArray")
public fun <T> ValidationBuilder<Array<T>>.onEach(init: ValidationBuilder<T>.() -> Unit) {
    val builder = ValidationBuilderImpl<T>()
    init(builder)
    run(ArrayValidation(builder.build()))
}

@JvmName("onEachMap")
public fun <K, V, T : Map<K, V>> ValidationBuilder<T>.onEach(init: ValidationBuilder<Map.Entry<K, V>>.() -> Unit) {
    val builder = ValidationBuilderImpl<Map.Entry<K, V>>()
    init(builder)
    @Suppress("UNCHECKED_CAST")
    run(MapValidation(builder.build()) as Validation<T>)
}
