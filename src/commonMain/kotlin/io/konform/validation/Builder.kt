package io.konform.validation

import kotlin.jvm.JvmName
import kotlin.reflect.KProperty1

@DslMarker
private annotation class ValidationScope

@ValidationScope
interface Builder<T, E> {
    val errors: MutableList<ErrorWithPath<E>>
    val path: Path
    val value: T
    val properties: Properties
    val mode: FailMode

    fun <R> KProperty1<T, R>.isInvalid(): Boolean

    /**
     * Returns true, if there is no errors for specified property.
     */
    fun <R> KProperty1<T, R>.isValid(): Boolean

    /**
     * Runs different validator in current context. Use [Validation.with] method to convert returned error type from validator to current error type of context.
     */
    fun run(validation: Validation<T, E>)

    /**
     * Runs validation block on each element of Iterable.
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("onEachIterable")
    infix fun <R> KProperty1<T, Iterable<R>>.onEach(block: BuilderBlock<R, E>)

    /**
     * Runs validation block on each element of Array.
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("onEachArray")
    infix fun <R> KProperty1<T, Array<R>>.onEach(block: BuilderBlock<R, E>)

    /**
     * Runs validation block on each map entry of Map.
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("onEachMap")
    infix fun <K, V> KProperty1<T, Map<K, V>>.onEach(block: BuilderBlock<Map.Entry<K, V>, E>)

    /**
     * Runs validation block only if property is not null.
     */
    infix fun <R : Any> KProperty1<T, R?>.ifPresent(block: BuilderBlock<R, E>)

    /**
     * Runs block, that will be executed, if property is changed. Changes list is removed for underlying checks.
     */
    infix fun <R> KProperty1<T, R>.affects(block: BuilderBlockWithArgument<T, E, R>): Unit?

    /**
     * Same as affect on single argument, but binds pair of arguments.
     */
    infix fun <R1, R2> Pair<KProperty1<T, R1>, KProperty1<T, R2>>.affects(block: BuilderBlockWithArgument<T, E, Pair<R1, R2>>): Unit?

    /**
     * Checks that property is not null. If not - [constructError] will be called and error added to context.
     */
    fun <R : Any> KProperty1<T, R?>.required(
        constructError: ErrorConstructor<R?, E>,
        block: BuilderBlock<R, E> = {}
    ): Unit?

    /**
     * Creates context around validated property and executes validation on its value.
     */
    operator fun <R> KProperty1<T, R>.invoke(block: BuilderBlock<R, E>): Unit?

    /**
     * Delegates validation of property to external validator. Use [Validation.with] method to convert returned error type from validator to current error type of context.
     */
    infix fun <R> KProperty1<T, R>.by(validation: Validation<R, E>)

    /**
     * Deferred check function for helpers. See helpers in `Checks.kt`
     */
    fun check(predicate: (T) -> Boolean, constructError: ErrorConstructor<T, E>)

    /**
     * Add error (with corrected property path) to validation context.
     */
    fun fail(error: E)

    /**
     * Runs validation block until first error occurred.
     */
    fun eager(block: BuilderBlock<T, E>): Unit?
}

typealias BuilderBlock<T, E> = Builder<T, E>.() -> Unit

typealias BuilderBlockWithArgument<T, E, R> = Builder<T, E>.(R) -> Unit
