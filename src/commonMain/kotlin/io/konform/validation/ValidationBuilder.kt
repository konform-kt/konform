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
abstract class ValidationBuilder<T> {
    abstract fun build(): Validation<T>
    abstract fun addConstraint(errorMessage: String, vararg templateValues: String, test: (T) -> Boolean): Constraint<T>
    abstract infix fun Constraint<T>.hint(hint: String): Constraint<T>
    abstract operator fun <R> ((T) -> R).invoke(name: String, init: ValidationBuilder<R>.() -> Unit)
    internal abstract fun <R> onEachIterable(name: String, prop: (T) -> Iterable<R>, init: ValidationBuilder<R>.() -> Unit)
    internal abstract fun <R> onEachArray(name: String, prop: (T) -> Array<R>, init: ValidationBuilder<R>.() -> Unit)
    internal abstract fun <K, V> onEachMap(name: String, prop: (T) -> Map<K, V>, init: ValidationBuilder<Map.Entry<K, V>>.() -> Unit)
    abstract fun <R> ((T) -> R?).ifPresent(name: String, init: ValidationBuilder<R>.() -> Unit)
    abstract fun <R> ((T) -> R?).required(name: String, init: ValidationBuilder<R>.() -> Unit)
    operator fun <R> KProperty1<T, R>.invoke(init: ValidationBuilder<R>.() -> Unit) = invoke(name, init)
    operator fun <R> KFunction1<T, R>.invoke(init: ValidationBuilder<R>.() -> Unit) = invoke("$name()", init)
    @JvmName("onEachArray")
    infix fun <R> KProperty1<T, Array<R>>.onEach(init: ValidationBuilder<R>.() -> Unit) = onEachArray(name, this, init)
    @JvmName("onEachIterable")
    infix fun <R> KProperty1<T, Iterable<R>>.onEach(init: ValidationBuilder<R>.() -> Unit) = onEachIterable(name, this, init)
    @JvmName("onEachMap")
    infix fun <K, V> KProperty1<T, Map<K, V>>.onEach(init: ValidationBuilder<Map.Entry<K, V>>.() -> Unit) = onEachMap(name, this, init)
    @JvmName("onEachArray")
    infix fun <R> KFunction1<T, Array<R>>.onEach(init: ValidationBuilder<R>.() -> Unit) = onEachArray("$name()", this, init)
    @JvmName("onEachIterable")
    infix fun <R> KFunction1<T, Iterable<R>>.onEach(init: ValidationBuilder<R>.() -> Unit) = onEachIterable("$name()", this, init)
    @JvmName("onEachMap")
    infix fun <K, V> KFunction1<T, Map<K, V>>.onEach(init: ValidationBuilder<Map.Entry<K, V>>.() -> Unit) = onEachMap("$name()", this, init)
    infix fun <R> KProperty1<T, R?>.ifPresent(init: ValidationBuilder<R>.() -> Unit) = ifPresent(name, init)
    infix fun <R> KProperty1<T, R?>.required(init: ValidationBuilder<R>.() -> Unit) = required(name, init)
    infix fun <R> KFunction1<T, R?>.ifPresent(init: ValidationBuilder<R>.() -> Unit) = ifPresent("$name()", init)
    infix fun <R> KFunction1<T, R?>.required(init: ValidationBuilder<R>.() -> Unit) = required("$name()", init)
    abstract fun run(validation: Validation<T>)
    abstract val <R> KProperty1<T, R>.has: ValidationBuilder<R>
    abstract val <R> KFunction1<T, R>.has: ValidationBuilder<R>
}

fun <T : Any> ValidationBuilder<T?>.ifPresent(init: ValidationBuilder<T>.() -> Unit) {
    val builder = ValidationBuilderImpl<T>()
    init(builder)
    run(OptionalValidation(builder.build()))
}

fun <T : Any> ValidationBuilder<T?>.required(init: ValidationBuilder<T>.() -> Unit) {
    val builder = ValidationBuilderImpl<T>()
    init(builder)
    run(RequiredValidation(builder.build()))
}

@JvmName("onEachIterable")
fun <S, T : Iterable<S>> ValidationBuilder<T>.onEach(init: ValidationBuilder<S>.() -> Unit) {
    val builder = ValidationBuilderImpl<S>()
    init(builder)
    @Suppress("UNCHECKED_CAST")
    run(IterableValidation(builder.build()) as Validation<T>)
}

@JvmName("onEachArray")
fun <T> ValidationBuilder<Array<T>>.onEach(init: ValidationBuilder<T>.() -> Unit) {
    val builder = ValidationBuilderImpl<T>()
    init(builder)
    @Suppress("UNCHECKED_CAST")
    run(ArrayValidation(builder.build()) as Validation<Array<T>>)
}

@JvmName("onEachMap")
fun <K, V, T : Map<K, V>> ValidationBuilder<T>.onEach(init: ValidationBuilder<Map.Entry<K, V>>.() -> Unit) {
    val builder = ValidationBuilderImpl<Map.Entry<K, V>>()
    init(builder)
    @Suppress("UNCHECKED_CAST")
    run(MapValidation(builder.build()) as Validation<T>)
}
