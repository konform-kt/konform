package io.konform.validation.path

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ValidationPathTest {
    @Test
    fun fromAny() {
        ValidationPath.of("abc", List<*>::isEmpty, 1) shouldBe
            ValidationPath(
                listOf(PathValue("abc"), FuncRef(List<*>::isEmpty), PathIndex(1)),
            )
    }

    @Test
    fun appendPrepend() {
        val base = ValidationPath.of("abc", "def")

        base + PathSegment.toPathSegment("ghj") shouldBe ValidationPath.of("abc", "def", "ghj")
        base + ValidationPath.of(1, 2) shouldBe ValidationPath.of("abc", "def", 1, 2)
        base.prepend(PathSegment.toPathSegment(0)) shouldBe ValidationPath.of(0, "abc", "def")
        base.prepend(ValidationPath.of(1, 2)) shouldBe ValidationPath.of(1, 2, "abc", "def")
    }
}
