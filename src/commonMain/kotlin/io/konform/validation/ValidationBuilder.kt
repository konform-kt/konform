package io.konform.validation

import io.konform.validation.ValidationBuilder.Companion.buildWithNew
import io.konform.validation.builders.RequiredValidationBuilder
import io.konform.validation.helpers.prepend
import io.konform.validation.path.FuncRef
import io.konform.validation.path.PathSegment
import io.konform.validation.path.PropRef
import io.konform.validation.path.ValidationPath
import io.konform.validation.types.ArrayValidation
import io.konform.validation.types.CallableValidation
import io.konform.validation.types.ConstraintsValidation
import io.konform.validation.types.DynamicCallableValidation
import io.konform.validation.types.DynamicValidation
import io.konform.validation.types.IsClassValidation
import io.konform.validation.types.IterableValidation
import io.konform.validation.types.MapValidation
import kotlin.js.JsName
import kotlin.jvm.JvmName
import kotlin.reflect.KFunction1
import kotlin.reflect.KProperty1

@DslMarker
private annotation class ValidationScope

@ValidationScope
// Class is open to users can define their extra local extension methods
public open class ValidationBuilder<T> {
    protected val constraints: MutableList<Constraint<T>> = mutableListOf()
    protected val subValidations: MutableList<Validation<T>> = mutableListOf()

    /**
     * Override the path that will be used for sub-validations and constraints in this builder.
     * When set, this path replaces the default path that would normally be generated.
     *
     * This is useful for inline/value classes where the wrapper should be transparent in error paths,
     * or other scenarios where the default path doesn't match the serialized structure.
     *
     * Example:
     * ```kotlin
     * WrapperClass::valueClass {
     *   path = ValidationPath.EMPTY  // Remove this path segment
     *   ValueClass::integer {
     *     minimum(1)
     *   }
     * }
     * ```
     */
    public var path: ValidationPath? = null

    public open fun build(): Validation<T> =
        subValidations
            .let {
                if (constraints.isNotEmpty()) {
                    it.prepend(ConstraintsValidation(constraints))
                } else {
                    it
                }
            }.flatten()

    // region constraints
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
    // endregion

    // region onEach
    private fun <R> onEachIterable(
        pathSegment: PathSegment,
        prop: (T) -> Iterable<R>,
        init: ValidationBuilder<R>.() -> Unit,
    ) {
        val builder = ValidationBuilder<R>()
        init(builder)
        val actualPath = builder.path ?: ValidationPath.of(pathSegment)
        run(CallableValidation(actualPath, prop, IterableValidation(builder.build())))
    }

    private fun <R> onEachArray(
        pathSegment: PathSegment,
        prop: (T) -> Array<R>,
        init: ValidationBuilder<R>.() -> Unit,
    ) {
        val builder = ValidationBuilder<R>()
        init(builder)
        val actualPath = builder.path ?: ValidationPath.of(pathSegment)
        run(CallableValidation(actualPath, prop, ArrayValidation(builder.build())))
    }

    private fun <K, V> onEachMap(
        pathSegment: PathSegment,
        prop: (T) -> Map<K, V>,
        init: ValidationBuilder<Map.Entry<K, V>>.() -> Unit,
    ) {
        val builder = ValidationBuilder<Map.Entry<K, V>>()
        init(builder)
        val actualPath = builder.path ?: ValidationPath.of(pathSegment)
        run(CallableValidation(actualPath, prop, MapValidation(builder.build())))
    }

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
    // endregion

    // region Callable infix
    public operator fun <R> KProperty1<T, R>.invoke(init: ValidationBuilder<R>.() -> Unit): Unit = validate(this, this, init)

    public operator fun <R> KFunction1<T, R>.invoke(init: ValidationBuilder<R>.() -> Unit): Unit = validate(this, this, init)

    /** Run a validation on the result of this property, but only if it's not null. */
    public infix fun <R : Any> KProperty1<T, R?>.ifPresent(init: ValidationBuilder<R>.() -> Unit): Unit = ifPresent(this, this, init)

    @JsName("ifPresentOnNotNullProperty")
    @JvmName("ifPresentOnNotNullProperty")
    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("ifPresent has no effect on not-null property, can be removed.")
    public infix fun <R : Any> KProperty1<T, R>.ifPresent(init: ValidationBuilder<R>.() -> Unit): Unit =
        (this as KProperty1<T, R?>).ifPresent(init)

    // Don't deprecate calling this on not null props, to ease working with functions returning platform types

    /** Run a validation on the result of this function, but only if it's not null. */
    public infix fun <R : Any> KFunction1<T, R?>.ifPresent(init: ValidationBuilder<R>.() -> Unit): Unit = ifPresent(this, this, init)

    /** Validate that the result of this property is not null and run a validation on it. */
    public infix fun <R : Any> KProperty1<T, R?>.required(init: RequiredValidationBuilder<R>.() -> Unit): Unit = required(this, this, init)

    @JsName("requiredOnNotNullProperty")
    @JvmName("requiredOnNotNullProperty")
    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("required has no effect on not-null property, can be removed.")
    public infix fun <R : Any> KProperty1<T, R>.required(init: RequiredValidationBuilder<R>.() -> Unit): Unit =
        (this as KProperty1<T, R?>).required(init)

    // Don't deprecate calling this on not null props, to ease working with functions returning platform types

    /** Validate that the result of this function is not null and run a validation on it. */
    public infix fun <R : Any> KFunction1<T, R?>.required(init: ValidationBuilder<R>.() -> Unit): Unit = required(this, this, init)

    public infix fun <R> KProperty1<T, R>.dynamic(init: ValidationBuilder<R>.(T) -> Unit): Unit = dynamic(this, this, init)

    public infix fun <R> KFunction1<T, R>.dynamic(init: ValidationBuilder<R>.(T) -> Unit): Unit = dynamic(this, this, init)
    // endregion

    // region transform

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
    ) {
        val builder = ValidationBuilder<R>()
        init(builder)
        val actualPath = builder.path ?: ValidationPath.of(path)
        run(CallableValidation(actualPath, f, builder.build()))
    }

    /**
     * Build a new validation based on a transformed value of the input and run it.
     * @param path The [PathSegment] or [ValidationPath] of the validation.
     *   is [Any] for backwards compatibility and ease of use, see [ValidationPath.of].
     * @see validate
     * */
    public fun <R> dynamic(
        path: Any,
        f: (T) -> R,
        init: ValidationBuilder<R>.(T) -> Unit,
    ): Unit = run(DynamicCallableValidation(ValidationPath.of(path), f, init))

    /** Build a new validation based on the current value being validated and run it. */
    public fun dynamic(init: ValidationBuilder<T>.(T) -> Unit): Unit = dynamic(ValidationPath.EMPTY, { it }, init)

    /**
     * Calculate a value from the input and run a validation on it, but only if the value is not null.
     * @param path The [PathSegment] or [ValidationPath] of the validation.
     *   is [Any] for backwards compatibility and ease of use, see [ValidationPath.of].
     */
    public fun <R> ifPresent(
        path: Any,
        f: (T) -> R?,
        init: ValidationBuilder<R>.() -> Unit,
    ) {
        val builder = ValidationBuilder<R>()
        init(builder)
        val actualPath = builder.path ?: ValidationPath.of(path)
        run(CallableValidation(actualPath, f, builder.build().ifPresent()))
    }

    /**
     * Calculate a value from the input and run a validation on it, and give an error if the result is null.
     * @param path The [PathSegment] or [ValidationPath] of the validation.
     *   is [Any] for backwards compatibility and ease of use, see [ValidationPath.of].
     */
    public fun <R : Any> required(
        path: Any,
        f: (T) -> R?,
        init: RequiredValidationBuilder<R>.() -> Unit,
    ) {
        val builder = RequiredValidationBuilder<R>()
        init(builder)
        val actualPath = builder.path ?: ValidationPath.of(path)
        run(CallableValidation(actualPath, f, builder.build()))
    }

    // endregion

    // region run
    public fun run(validation: Validation<T>) {
        subValidations.add(validation)
    }

    /** Create a validation based on the current value being validated and run it. */
    public fun runDynamic(creator: (T) -> Validation<T>) {
        run(DynamicValidation(creator))
    }
    // endregion

    // region subtypes
    public inline fun <reified SubT : T & Any> ifInstanceOf(init: ValidationBuilder<SubT>.() -> Unit): Unit =
        run(IsClassValidation<SubT, T>(SubT::class, required = false, buildWithNew(init)))

    public inline fun <reified SubT : T & Any> requireInstanceOf(init: ValidationBuilder<SubT>.() -> Unit): Unit =
        run(IsClassValidation<SubT, T>(SubT::class, required = true, buildWithNew(init)))
    // endregion

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
