package io.konform.validation.types

import io.konform.validation.Invalid
import io.konform.validation.Validation
import io.konform.validation.ValidationResult
import io.konform.validation.flattenOrValid
import io.konform.validation.path.PathSegment

internal class MapValidation<K, V>(
    private val validation: Validation<Map.Entry<K, V>>,
) : Validation<Map<K, V>> {
    override fun validate(value: Map<K, V>): ValidationResult<Map<K, V>> {
        val errors = mutableListOf<Invalid>()
        value.forEach {
            val result = validation.validate(it)
            if (result is Invalid) {
                errors += result.prependPath(PathSegment.MapKey(it.key))
            }
        }
        return errors.flattenOrValid(value)
    }
}

