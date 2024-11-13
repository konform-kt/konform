package io.konform.validation.path

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ValidationPathTest {
    @Test
    fun fromAny() {
        ValidationPath.fromAny("abc", List<*>::isEmpty, 1) shouldBe
            ValidationPath(
                listOf(PathValue("abc"), FuncRef(List<*>::isEmpty), PathIndex(1)),
            )
    }

    @Test
    fun appendPrepend() {
        val base = ValidationPath.fromAny("abc", "def")

        base + PathSegment.toPathSegment("ghj") shouldBe ValidationPath.fromAny("abc", "def", "ghj")
        base + ValidationPath.fromAny(1, 2) shouldBe ValidationPath.fromAny("abc", "def", 1, 2)
        base.prepend(PathSegment.toPathSegment(0)) shouldBe ValidationPath.fromAny(0, "abc", "def")
        base.prepend(ValidationPath.fromAny(1, 2)) shouldBe ValidationPath.fromAny(1, 2, "abc", "def")
    }
}
