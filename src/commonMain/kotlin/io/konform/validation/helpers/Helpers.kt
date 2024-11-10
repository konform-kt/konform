package io.konform.validation.helpers

internal fun <T> List<T>.prepend(element: T): List<T> {
    if (isEmpty()) return listOf(element)
    val result = ArrayList<T>(size + 1)
    result.add(element)
    result.addAll(this)
    return result
}
