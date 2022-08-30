package io.konform.validation

import kotlin.jvm.JvmName
import kotlin.reflect.KProperty1

// TODO: Make this exception stacktrace-less
internal open class NoStackTraceRuntimeException(message: String) : Throwable(message)
internal object StopValidation : NoStackTraceRuntimeException("")

typealias ErrorConstructor<T, E> = (T) -> E

enum class FailMode {
    SOFT,
    HARD
}

class ValidationBuilder<T, E>(
    override val errors: MutableList<ErrorWithPath<E>>,
    override val path: Path,
    override val value: T,
    override val properties: Properties,
    override val mode: FailMode = FailMode.SOFT,
    private val parent: Builder<*, E>? = null
) : Builder<T, E> {
    override fun <R> KProperty1<T, R>.invoke(block: BuilderBlock<R, E>) =
        subValidation(path + Property(this), this(value))?.runBlock(block)

    override fun <R> KProperty1<T, R>.isInvalid() =
        errors.any { (p, _) -> path + Property(this) == p }

    override fun <R> KProperty1<T, R>.isValid() = !this.isInvalid()

    override fun run(validation: Validation<T, E>) =
        this.runBlock(validation.block)

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("onEachIterable")
    override fun <R> KProperty1<T, Iterable<R>>.onEach(block: BuilderBlock<R, E>) {
        this(value).onEachIndexed { index, item ->
            subValidation(
                path + PropertyElement(Property(this), Index(index)),
                item
            )?.runBlock(block)
        }
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("onEachArray")
    override fun <R> KProperty1<T, Array<R>>.onEach(block: BuilderBlock<R, E>) {
        this(value).onEachIndexed { index, item ->
            subValidation(
                path + PropertyElement(Property(this), Index(index)),
                item
            )?.runBlock(block)
        }
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("onEachMap")
    override fun <K, V> KProperty1<T, Map<K, V>>.onEach(block: BuilderBlock<Map.Entry<K, V>, E>) {
        this(value).onEach { entry ->
            subValidation(path + PropertyElement(Property(this), Key("${entry.key}")), entry)?.runBlock(
                block
            )
        }
    }

    override fun <R : Any> KProperty1<T, R?>.ifPresent(block: BuilderBlock<R, E>) {
        val property = this(value)
        if (property != null) {
            subValidation(path + Property(this), property)?.runBlock(block)
        }
    }

    override fun <R : Any> KProperty1<T, R?>.required(
        constructError: ErrorConstructor<R?, E>,
        block: BuilderBlock<R, E>
    ) = when (val property = this(value)) {
        null -> fail(constructError(null))
        else -> subValidation(path + Property(this), property)?.runBlock(block)
    }

    override fun <R> KProperty1<T, R>.affects(block: BuilderBlockWithArgument<T, E, R>) =
        subValidation(path, value, Properties.Any)?.runBlock(block, this(value))

    override fun <R1, R2> Pair<KProperty1<T, R1>, KProperty1<T, R2>>.affects(block: BuilderBlockWithArgument<T, E, Pair<R1, R2>>) =
        subValidation(path, value, Properties.Any)?.runBlock(block, Pair(this.first(value), this.second(value)))

    override fun <R> KProperty1<T, R>.by(validation: Validation<R, E>) {
        subValidation(path + Property(this), this(value))?.runBlock(validation.block)
    }

    override fun check(predicate: (T) -> Boolean, constructError: ErrorConstructor<T, E>) {
        if (!predicate(value)) {
            fail(constructError(value))
        }
    }

    override fun fail(error: E) {
        errors.add(ErrorWithPath(path, error))
        if (parent.shouldReceiveException()) {
            throw StopValidation
        }
    }

    fun runBlock(block: BuilderBlock<T, E>) {
        try {
            block()
        } catch (e: StopValidation) {
            if (parent.shouldReceiveException()) {
                throw e
            }
        }
    }

    private fun <R> runBlock(block: BuilderBlockWithArgument<T, E, R>, value: R) {
        try {
            block(value)
        } catch (e: StopValidation) {
            if (parent.shouldReceiveException()) {
                throw e
            }
        }
    }

    override fun eager(block: BuilderBlock<T, E>) = try {
        subValidation(path, value, mode = FailMode.HARD)?.runBlock(block)
    } catch (_: StopValidation) {
    }
}

@JvmName("onEachIterable")
fun <R, E> Builder<out Iterable<R>, E>.onEach(block: BuilderBlock<R, E>) {
    value.onEachIndexed { i, it ->
        subValidation(path + Index(i), it, properties)?.runBlock(block)
    }
}

@JvmName("onEachArray")
fun <R, E> Builder<Array<R>, E>.onEach(block: BuilderBlock<R, E>) {
    value.onEachIndexed { i, it ->
        subValidation(path + Index(i), it, properties)?.runBlock(block)
    }
}

@JvmName("onEachMap")
fun <K, V, E> Builder<Map<K, V>, E>.onEach(block: BuilderBlock<Map.Entry<K, V>, E>) {
    value.onEach { entry ->
        subValidation(path + Key("${entry.key}"), entry)?.runBlock(block)
    }
}

fun <R, E> Builder<R?, E>.ifPresent(block: BuilderBlock<R, E>) {
    value?.let {
        subValidation<R, E>(path, it)?.runBlock(block)
    }
}

fun <R, E> Builder<R?, E>.required(
    constructError: ErrorConstructor<R?, E>,
    block: BuilderBlock<R, E>
) = when (value) {
    null -> fail(constructError(null))
    else -> subValidation<R, E>(path, value!!)?.runBlock(block)
}

internal fun <R, E> Builder<*, E>.subValidation(
    subPath: Path,
    value: R,
    properties: Properties = this.properties,
    mode: FailMode = FailMode.SOFT
): ValidationBuilder<R, E>? = when (properties contains subPath) {
    true -> ValidationBuilder(errors, subPath, value, properties, parent = this, mode = mode)
    false -> null
}

private fun <T, E> Builder<T, E>?.shouldReceiveException() = this?.mode != FailMode.SOFT
