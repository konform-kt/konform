package io.konform.validation.path

import io.konform.validation.platform.callableEquals
import kotlin.reflect.KFunction1

/**
 * Represents a function in the path.
 * Note: equality differs between platforms, on JS & WASM, only the function name is considered
 */
public data class FuncRef(
    val function: KFunction1<*, *>,
) : PathSegment {
    override val pathString: String get() = ".${function.name}"

    override fun toString(): String = "FuncRef(${function.name})"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as FuncRef
        return callableEquals(function, other.function)
    }

    override fun hashCode(): Int = function.name.hashCode()
}

public fun KFunction1<*, *>.toPathSegment(): FuncRef = FuncRef(this)
