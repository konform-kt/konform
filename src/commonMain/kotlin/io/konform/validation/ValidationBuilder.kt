package io.konform.validation

import io.konform.validation.internal.ArrayValidation
import io.konform.validation.internal.IterableValidation
import io.konform.validation.internal.MapValidation
import io.konform.validation.internal.OptionalValidation
import io.konform.validation.internal.RequiredValidation
import io.konform.validation.internal.ValidationBuilderImpl
import kotlin.jvm.JvmName
import kotlin.reflect.KProperty1

@DslMarker
private annotation class ValidationScope

@ValidationScope
abstract class ValidationBuilder<C, T> {
    abstract fun build(): Validation<C, T>
    abstract fun addConstraint(errorMessage: String, vararg templateValues: String, test: C.(T) -> Boolean): Constraint<C, T>
    abstract infix fun Constraint<C, T>.hint(hint: String): Constraint<C, T>
    abstract operator fun <R> KProperty1<T, R>.invoke(init: ValidationBuilder<C, R>.() -> Unit)
    internal abstract fun <R> onEachIterable(prop: KProperty1<T, Iterable<R>>, init: ValidationBuilder<C, R>.() -> Unit)
    @JvmName("onEachIterable")
    infix fun <R> KProperty1<T, Iterable<R>>.onEach(init: ValidationBuilder<C, R>.() -> Unit) = onEachIterable(this, init)
    internal abstract fun <R> onEachArray(prop: KProperty1<T, Array<R>>, init: ValidationBuilder<C, R>.() -> Unit)
    @JvmName("onEachArray")
    infix fun <R> KProperty1<T, Array<R>>.onEach(init: ValidationBuilder<C, R>.() -> Unit) = onEachArray(this, init)
    internal abstract fun <K, V> onEachMap(prop: KProperty1<T, Map<K, V>>, init: ValidationBuilder<C, Map.Entry<K, V>>.() -> Unit)
    @JvmName("onEachMap")
    infix fun <K, V> KProperty1<T, Map<K, V>>.onEach(init: ValidationBuilder<C, Map.Entry<K, V>>.() -> Unit) = onEachMap(this, init)
    abstract infix fun <R> KProperty1<T, R?>.ifPresent(init: ValidationBuilder<C, R>.() -> Unit)
    abstract infix fun <R> KProperty1<T, R?>.required(init: ValidationBuilder<C, R>.() -> Unit)
    abstract fun run(validation: Validation<C, T>)
    abstract fun <S> run(validation: Validation<S, T>, map: (C) -> S)
    abstract val <R> KProperty1<T, R>.has: ValidationBuilder<C, R>
}

fun <C, T : Any> ValidationBuilder<C, T?>.ifPresent(init: ValidationBuilder<C, T>.() -> Unit) {
    val builder = ValidationBuilderImpl<C, T>()
    init(builder)
    run(OptionalValidation(builder.build()))
}

fun <C, T : Any> ValidationBuilder<C, T?>.required(init: ValidationBuilder<C, T>.() -> Unit) {
    val builder = ValidationBuilderImpl<C, T>()
    init(builder)
    run(RequiredValidation(builder.build()))
}

@JvmName("onEachIterable")
fun <C, S, T : Iterable<S>> ValidationBuilder<C, T>.onEach(init: ValidationBuilder<C, S>.() -> Unit) {
    val builder = ValidationBuilderImpl<C, S>()
    init(builder)
    @Suppress("UNCHECKED_CAST")
    run(IterableValidation(builder.build()) as Validation<C, T>)
}

@JvmName("onEachArray")
fun <C, T> ValidationBuilder<C, Array<T>>.onEach(init: ValidationBuilder<C, T>.() -> Unit) {
    val builder = ValidationBuilderImpl<C, T>()
    init(builder)
    @Suppress("UNCHECKED_CAST")
    run(ArrayValidation(builder.build()) as Validation<C, Array<T>>)
}

@JvmName("onEachMap")
fun <C, K, V, T : Map<K, V>> ValidationBuilder<C, T>.onEach(init: ValidationBuilder<C, Map.Entry<K, V>>.() -> Unit) {
    val builder = ValidationBuilderImpl<C, Map.Entry<K, V>>()
    init(builder)
    @Suppress("UNCHECKED_CAST")
    run(MapValidation(builder.build()) as Validation<C, T>)
}
