package io.konform.validation.path

import io.kotest.inspectors.forAll
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertEquals

class PathSegmentTest {
    @Test
    fun toPathSegment() {
        val mappings =
            mapOf(
                "fieldName" to PathValue("fieldName"),
                1 to PathIndex(1),
                PathIndex(1) to PathIndex(1),
                PathSegmentTest::class to PathClass(PathSegmentTest::class),
                functionRef to FuncRef(functionRef),
                mapKeyRef to PropRef(mapKeyRef),
                null to PathValue(null),
            )

        mappings.forAll { (input, expected) ->
            PathSegment.toPathSegment(input) shouldBe expected
        }
    }

    @Test
    fun refFromDifferentContextShouldBeEqual() {
        // We saw some differing behavior here between different Kotlin targets (mainly JS/WASM vs rest)
        // when directly using callable references

        val ref2 = Map.Entry<*, *>::key

        assertEquals(PropRef(mapKeyRef), PropRef(ref2))
        assertEquals(PropRef(ref2), PropRef(mapKeyRef))
    }

    @Test
    fun valueAndMapKeyShouldBeEqual() {
        val pathValue = PathValue("abc")
        val pathKey = PathKey("abc")

        assertEquals<PathSegment>(pathKey, pathValue)
        assertEquals<PathSegment>(pathValue, pathKey)
        assertEquals(pathKey.hashCode(), pathValue.hashCode())
    }
}

private val mapKeyRef = Map.Entry<*, *>::key
private val functionRef = List<*>::isEmpty
