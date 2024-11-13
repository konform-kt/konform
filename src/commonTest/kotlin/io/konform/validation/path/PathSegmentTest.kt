package io.konform.validation.path

import io.kotest.inspectors.forAll
import io.kotest.matchers.shouldBe
import kotlin.test.Test

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
    fun mapKeyFromDifferentContextShouldBeEqual() {
        // We saw some differing behavior here between different Kotlin targets (mainly JS/WASM vs rest)
        // when directly using callable references

        val ref2 = Map.Entry<*, *>::key

        PathKey(mapKeyRef) shouldBe PathKey(ref2)
    }

    @Test
    fun valueAndMapKeyShouldBeEqual() {
        val pathValue = PathValue("abc")
        val pathKey = PathKey("abc")

        pathKey shouldBe pathValue
        pathValue shouldBe pathKey
        pathKey.hashCode() shouldBe pathValue.hashCode()
    }
}

private val mapKeyRef = Map.Entry<*, *>::key
private val functionRef = List<*>::isEmpty
