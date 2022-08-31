package io.konform.validation.errors

import io.konform.validation.Builder
import io.konform.validation.BuilderBlock
import io.konform.validation.Property
import io.konform.validation.subValidation
import kotlin.reflect.KProperty1

interface ValidationError {
    val message: String
}

data class Error(override val message: String) : ValidationError

fun <T, R : Any> Builder<T, ValidationError>.require(kproperty: KProperty1<T, R?>, block: BuilderBlock<R, ValidationError>) = when (val property = kproperty(value)) {
    null -> fail(Error("${kproperty.name} is required"))
    else -> subValidation(path + Property(kproperty), property)?.runBlock(block)
}
