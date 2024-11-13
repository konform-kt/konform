package io.konform.validation.platform

import kotlin.reflect.KCallable

// For rare cases where we need to behave different between platforms
internal actual fun callableEquals(
    first: KCallable<*>,
    second: KCallable<*>,
): Boolean = first.name.filter { it.isLetterOrDigit() } == second.name.filter { it.isLetterOrDigit() }
