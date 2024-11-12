package io.konform.validation.platform

import kotlin.reflect.KCallable

// For rare cases where we need to behave different between platforms
internal expect fun callableEquals(
    first: KCallable<*>,
    second: KCallable<*>,
): Boolean
