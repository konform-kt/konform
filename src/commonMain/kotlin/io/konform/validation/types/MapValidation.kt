package io.konform.validation.types

import io.konform.validation.Invalid
import io.konform.validation.Validation
import io.konform.validation.ValidationError
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
                errors += Invalid(result.errors.map { e -> setMapPath(it.key, e) })
            }
        }
        return errors.flattenOrValid(value)
    }

    private fun setMapPath(
        key: K,
        error: ValidationError,
    ): ValidationError {
        val keySegment = PathSegment.MapKey(key)
        return when (error.path.segments.firstOrNull()) {
            // Remove ".key" or ".value" to the path as usually we want
            // ".mapField.toStringKey.xxx" and not ".mapField.toStringKey.key.xxx"
            // or ".mapField.toStringKey.value.xxx"
            SEGMENT_MAP_KEY, SEGMENT_MAP_VALUE ->
                error.mapPath {
                    it.toMutableList().also { path -> path[0] = keySegment }
                }

            else -> error.prependPath(keySegment)
        }
    }

    private companion object {
        private val SEGMENT_MAP_KEY = PathSegment.Property(Map.Entry<*, *>::key)
        private val SEGMENT_MAP_VALUE = PathSegment.Property(Map.Entry<*, *>::value)
    }
}
