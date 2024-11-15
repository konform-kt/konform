package io.konform.validation

import io.konform.validation.ValidationBuilder.Companion.buildWithNew
import io.konform.validation.helpers.prepend
import io.konform.validation.path.FuncRef
import io.konform.validation.path.PathSegment
import io.konform.validation.path.PropRef
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
// Class is open to users can define their extra local extension methods
public open class ValidationBuilder<T> {
    private val constraints = mutableListOf<Constraint<T>>()
    private val subValidations = mutableListOf<Validation<T>>()

    public fun build(): Validation<T> =
        subValidations
            .let {
                if (constraints.isNotEmpty()) {
                    it.prepend(ConstraintsValidation(constraints))
                } else {
                    it
                }
            }.flatten()

    @Deprecated(
        "Use constrain(), templateValues are no longer supported, put them directly in the hint",
        ReplaceWith("constrain(errorMessage, test)"),
    )
    public fun addConstraint(
        errorMessage: String,
        vararg templateValues: String,
        test: (T) -> Boolean,
    ): Constraint<T> = applyConstraint(Constraint(errorMessage, templateValues = templateValues.toList(), test = test))

    /** Add a new [Constraint] to this validation. */
    public fun constrain(
        hint: String,
        path: ValidationPath = ValidationPath.EMPTY,
        userContext: Any? = null,
        test: (T) -> Boolean,
    ): Constraint<T> = applyConstraint(Constraint(hint, path, userContext, emptyList(), test))

    /** Replace one or more properties of a [Constraint]. */
    public fun Constraint<T>.replace(
        hint: String = this.hint,
        path: ValidationPath = this.path,
        userContext: Any? = this.userContext,
    ): Constraint<T> = replaceConstraint(this, this.copy(hint = hint, path = path, userContext = userContext))

    /** Change the hint on a [Constraint]. */
    public infix fun Constraint<T>.hint(hint: String): Constraint<T> = replaceConstraint(this, this.copy(hint = hint))

    /** Change the path on a [Constraint]. */
    public infix fun Constraint<T>.path(path: ValidationPath): Constraint<T> = replaceConstraint(this, this.copy(path = path))

    /** Change the userContext on a [Constraint]. */
    public infix fun Constraint<T>.userContext(userContext: Any?): Constraint<T> =
        replaceConstraint(this, this.copy(userContext = userContext))

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
        onEachIterable(PropRef(this), this, init)

    @JvmName("onEachIterable")
    public infix fun <R> KFunction1<T, Iterable<R>>.onEach(init: ValidationBuilder<R>.() -> Unit): Unit =
        onEachIterable(FuncRef(this), this, init)

    @JvmName("onEachArray")
    public infix fun <R> KProperty1<T, Array<R>>.onEach(init: ValidationBuilder<R>.() -> Unit): Unit =
        onEachArray(PropRef(this), this, init)

    @JvmName("onEachArray")
    public infix fun <R> KFunction1<T, Array<R>>.onEach(init: ValidationBuilder<R>.() -> Unit): Unit =
        onEachArray(FuncRef(this), this, init)

    @JvmName("onEachMap")
    public infix fun <K, V> KProperty1<T, Map<K, V>>.onEach(init: ValidationBuilder<Map.Entry<K, V>>.() -> Unit): Unit =
        onEachMap(PropRef(this), this, init)

    @JvmName("onEachMap")
    public infix fun <K, V> KFunction1<T, Map<K, V>>.onEach(init: ValidationBuilder<Map.Entry<K, V>>.() -> Unit): Unit =
        onEachMap(FuncRef(this), this, init)

    public operator fun <R> KProperty1<T, R>.invoke(init: ValidationBuilder<R>.() -> Unit): Unit = validate(this, this, init)

    public operator fun <R> KFunction1<T, R>.invoke(init: ValidationBuilder<R>.() -> Unit): Unit = validate(this, this, init)

    public infix fun <R> KProperty1<T, R?>.ifPresent(init: ValidationBuilder<R>.() -> Unit): Unit = ifPresent(this, this, init)

    public infix fun <R> KFunction1<T, R?>.ifPresent(init: ValidationBuilder<R>.() -> Unit): Unit = ifPresent(this, this, init)

    public infix fun <R> KProperty1<T, R?>.required(init: ValidationBuilder<R>.() -> Unit): Unit = required(this, this, init)

    public infix fun <R> KFunction1<T, R?>.required(init: ValidationBuilder<R>.() -> Unit): Unit = required(this, this, init)

    /**
     * Calculate a value from the input and run a validation on it.
     * @param path The [PathSegment] or [ValidationPath] of the validation.
     *   is [Any] for backwards compatibility and ease of use, see [ValidationPath.of].
     * @param f The function for which you want to validate the result of
     */
    public fun <R> validate(
        path: Any,
        f: (T) -> R,
        init: ValidationBuilder<R>.() -> Unit,
    ): Unit = run(CallableValidation(path, f, buildWithNew(init)))

    /**
     * Calculate a value from the input and run a validation on it, but only if the value is not null.
     * @param path The [PathSegment] or [ValidationPath] of the validation.
     *   is [Any] for backwards compatibility and ease of use, see [ValidationPath.of].
     */
    public fun <R> ifPresent(
        path: Any,
        f: (T) -> R?,
        init: ValidationBuilder<R>.() -> Unit,
    ): Unit = run(CallableValidation(path, f, buildWithNew(init).ifPresent()))

    /**
     * Calculate a value from the input and run a validation on it, and give an error if the result is null.
     * @param path The [PathSegment] or [ValidationPath] of the validation.
     *   is [Any] for backwards compatibility and ease of use, see [ValidationPath.of].
     */
    public fun <R> required(
        path: Any,
        f: (T) -> R?,
        init: ValidationBuilder<R>.() -> Unit,
    ): Unit = run(CallableValidation(path, f, buildWithNew(init).required()))

    public fun run(validation: Validation<T>) {
        subValidations.add(validation)
    }

    /** Add a [Constraint] and return it. */
    public fun applyConstraint(constraint: Constraint<T>): Constraint<T> {
        constraints.add(constraint)
        return constraint
    }

    /** Replace a [Constraint] and return the replacement */
    public fun replaceConstraint(
        old: Constraint<T>,
        replacement: Constraint<T>,
    ): Constraint<T> {
        // It's very likely that the last added constraint is the one to be replaced so optimize for that
        val idx = if (constraints.lastOrNull() === old) constraints.size - 1 else constraints.indexOf(old)
        if (idx == -1) throw IllegalArgumentException("Not found in existing constraints: $old")
        constraints[idx] = replacement
        return replacement
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
