package io.konform.validation.constraints

import io.konform.validation.Valid
import io.konform.validation.Validation
import io.konform.validation.ValidationResult
import io.konform.validation.constraints.ConstraintsTest.TCPPacket.ACK
import io.konform.validation.constraints.ConstraintsTest.TCPPacket.SYN
import io.konform.validation.constraints.ConstraintsTest.TCPPacket.SYNACK
import io.konform.validation.countFieldsWithErrors
import io.konform.validation.path.ValidationPath
import io.kotest.assertions.konform.shouldBeInvalid
import io.kotest.assertions.konform.shouldBeValid
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldMatch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Suppress("DEPRECATION")
class ConstraintsTest {
    @Test
    fun typeConstraint() {
        val anyValidation = Validation<Any> { type<String>() }
        assertEquals<ValidationResult<Any>>(
            Valid("This is a string"),
            anyValidation("This is a string"),
        )

        assertEquals(1, countFieldsWithErrors(anyValidation(1)))
        assertEquals(1, countFieldsWithErrors(anyValidation(1.0)))
        assertEquals(1, countFieldsWithErrors(anyValidation(true)))

        val anyNumberValidation = Validation<Any> { type<Int>() }
        assertEquals<ValidationResult<Any>>(Valid(1), anyNumberValidation(1))
        assertEquals(1, countFieldsWithErrors(anyNumberValidation("String")))
        assertEquals(1, countFieldsWithErrors(anyNumberValidation(true)))

        assertEquals("must be of type 'String'", anyValidation(1).get()[0])
        assertEquals("must be of type 'Int'", anyNumberValidation("String").get()[0])
    }

    @Test
    fun nullableTypeConstraint() {
        val anyValidation = Validation<Any?> { type<String?>() }
        assertEquals<ValidationResult<Any?>>(
            Valid("This is a string"),
            anyValidation("This is a string"),
        )
        assertEquals<ValidationResult<Any?>>(
            Valid(null),
            anyValidation(null),
        )
    }

    @Test
    fun stringEnumConstraint() {
        val validation = Validation<String> { enum("OK", "CANCEL") }
        assertEquals(Valid("OK"), validation("OK"))
        assertEquals(Valid("CANCEL"), validation("CANCEL"))
        assertEquals(1, countFieldsWithErrors(validation("???")))
        assertEquals(1, countFieldsWithErrors(validation("")))

        assertEquals("must be one of: 'OK', 'CANCEL'", validation("").get()[0])
    }

    enum class TCPPacket {
        SYN,
        ACK,
        SYNACK,
    }

    @Test
    fun kotlinEnumConstraint() {
        val partialEnumValidation = Validation<TCPPacket> { enum(SYN, ACK) }
        assertEquals(Valid(SYN), partialEnumValidation(SYN))
        assertEquals(Valid(ACK), partialEnumValidation(ACK))
        assertEquals(1, countFieldsWithErrors(partialEnumValidation(SYNACK)))

        val stringifiedEnumValidation = Validation<String> { enum<TCPPacket>() }
        assertEquals(Valid("SYN"), stringifiedEnumValidation("SYN"))
        assertEquals(Valid("ACK"), stringifiedEnumValidation("ACK"))
        assertEquals(Valid("SYNACK"), stringifiedEnumValidation("SYNACK"))
        assertEquals(1, countFieldsWithErrors(stringifiedEnumValidation("ASDF")))

        assertEquals("must be one of: 'SYN', 'ACK'", partialEnumValidation(SYNACK).get()[0])
        assertEquals("must be one of: 'SYN', 'ACK', 'SYNACK'", stringifiedEnumValidation("").get()[0])
    }

    @Test
    fun constConstraint() {
        val validation = Validation<String> { const("Konform") }
        assertEquals(Valid("Konform"), validation("Konform"))
        assertEquals(1, countFieldsWithErrors(validation("")))

        val nullableConstNullValidation = Validation<String?> { const(null) }
        assertEquals<ValidationResult<String?>>(Valid(null), nullableConstNullValidation(null))
        assertEquals(1, countFieldsWithErrors(nullableConstNullValidation("")))
        assertEquals(1, countFieldsWithErrors(nullableConstNullValidation("Konform")))

        val nullableConstValidation = Validation<String?> { const("Konform") }
        assertEquals<ValidationResult<String?>>(Valid("Konform"), nullableConstValidation("Konform"))
        assertEquals(1, countFieldsWithErrors(nullableConstValidation(null)))
        assertEquals(1, countFieldsWithErrors(nullableConstValidation("Konverse")))

        assertEquals("must be 'Konform'", validation("Konverse").get()[0])
        assertEquals("must be 'null'", nullableConstNullValidation("Konform").get()[0])
        assertEquals("must be 'Konform'", nullableConstValidation(null).get()[0])
    }

    @Test
    fun multipleOfConstraint() {
        val validation = Validation<Number> { multipleOf(2.5) }
        assertEquals<ValidationResult<Number>>(Valid(0), validation(0))
        assertEquals<ValidationResult<Number>>(Valid(-2.5), validation(-2.5))
        assertEquals<ValidationResult<Number>>(Valid(2.5), validation(2.5))
        assertEquals<ValidationResult<Number>>(Valid(5), validation(5))
        assertEquals<ValidationResult<Number>>(Valid(25), validation(25))

        assertEquals(1, countFieldsWithErrors(validation(1)))
        assertEquals(1, countFieldsWithErrors(validation(1.0)))
        assertEquals(1, countFieldsWithErrors(validation(-4.0)))
        assertEquals(1, countFieldsWithErrors(validation(25.1)))

        assertFailsWith(IllegalArgumentException::class) { Validation<Number> { multipleOf(0) } }
        assertFailsWith(IllegalArgumentException::class) { Validation<Number> { multipleOf(-1) } }

        assertEquals("must be a multiple of '2.5'", validation(1).get()[0])
    }

    @Test
    fun maximumConstraint() {
        assertEquals(
            Valid<Int>(10),
            Validation<Int> { maximum(10) }(10),
        )

        assertEquals(
            Valid<Long>(10),
            Validation<Long> { maximum(10) }(10),
        )

        assertEquals(
            Valid<Float>(10.0f),
            Validation<Float> { maximum(10) }(10.0f),
        )

        assertEquals(
            Valid<Double>(10.0),
            Validation<Double> { maximum(10) }(10.0),
        )

        assertEquals(
            Valid<String>("a"),
            Validation<String> { maximum("b") }("a"),
        )

        assertEquals(
            Valid<String>("b"),
            Validation<String> { maximum("b") }("b"),
        )

        assertEquals(
            Valid(10.0),
            Validation<Double> { maximum(10) }(10.0),
        )

        val validation = Validation<Double> { maximum(10.0) }

        assertEquals(Valid(9.0), validation(9.0))
        assertEquals(Valid(10.0), validation(10.0))
        assertEquals(Valid(-10.0), validation(-10.0))
        assertEquals(Valid(Double.NEGATIVE_INFINITY), validation(Double.NEGATIVE_INFINITY))
        assertEquals(
            Valid(Double.POSITIVE_INFINITY),
            Validation { maximum(Double.POSITIVE_INFINITY) }(Double.POSITIVE_INFINITY),
        )

        assertEquals(1, countFieldsWithErrors(validation(10.00001)))
        assertEquals(1, countFieldsWithErrors(validation(11.0)))
        assertEquals(1, countFieldsWithErrors(validation(Double.POSITIVE_INFINITY)))

        val invalid = validation shouldBeInvalid 11.0
        invalid.errors shouldHaveSize 1
        // Small difference in numbers between kotlin JS and others
        invalid.errors[0].message shouldMatch "must be at most '10(\\.0)?'".toRegex()
    }

    @Test
    fun exclusiveMaximumConstraint() {
        assertEquals(
            Valid<Int>(9),
            Validation<Int> { exclusiveMaximum(10) }(9),
        )

        assertEquals(
            Valid<Long>(9),
            Validation<Long> { exclusiveMaximum(10) }(9),
        )

        assertEquals(
            Valid<Float>(9.0f),
            Validation<Float> { exclusiveMaximum(10) }(9.0f),
        )

        assertEquals(
            Valid<Double>(9.0),
            Validation<Double> { exclusiveMaximum(10) }(9.0),
        )

        assertEquals(
            Valid<String>("a"),
            Validation<String> { exclusiveMaximum("b") }("a"),
        )

        assertEquals(
            Valid<Double>(10.0),
            Validation<Double> { exclusiveMaximum(11) }(10.0),
        )

        val validation = Validation<Double> { exclusiveMaximum(10.0) }

        assertEquals(Valid(9.0), validation(9.0))
        assertEquals(Valid(9.99999999), validation(9.99999999))
        assertEquals(Valid(-10.0), validation(-10.0))
        assertEquals(Valid(Double.NEGATIVE_INFINITY), validation(Double.NEGATIVE_INFINITY))

        assertEquals(1, countFieldsWithErrors(validation(10.0)))
        assertEquals(1, countFieldsWithErrors(validation(10.00001)))
        assertEquals(1, countFieldsWithErrors(validation(11.0)))
        assertEquals(1, countFieldsWithErrors(validation(Double.POSITIVE_INFINITY)))
        assertEquals(
            1,
            countFieldsWithErrors(Validation<Double> { exclusiveMaximum(Double.POSITIVE_INFINITY) }(Double.POSITIVE_INFINITY)),
        )

        val invalid = validation shouldBeInvalid 11.0
        invalid.errors shouldHaveSize 1
        // Small difference in numbers between kotlin JS and others
        invalid.errors[0].message shouldMatch "must be less than '10(\\.0)?'".toRegex()
    }

    @Test
    fun minimumConstraint() {
        assertEquals(
            Valid<Int>(10),
            Validation<Int> { minimum(10) }(10),
        )

        assertEquals(
            Valid<Long>(10),
            Validation<Long> { minimum(10) }(10),
        )

        assertEquals(
            Valid<Float>(10.0f),
            Validation<Float> { minimum(10) }(10.0f),
        )

        assertEquals(
            Valid<Double>(10.0),
            Validation<Double> { minimum(10) }(10.0),
        )

        assertEquals(
            Valid<String>("b"),
            Validation<String> { minimum("a") }("b"),
        )

        assertEquals(
            Valid<String>("a"),
            Validation<String> { minimum("a") }("a"),
        )

        assertEquals(
            Valid<Double>(10.0),
            Validation<Double> { minimum(10) }(10.0),
        )

        val validation = Validation<Double> { minimum(10.0) }

        assertEquals(Valid(11.0), validation(11.0))
        assertEquals(Valid(10.0), validation(10.0))
        assertEquals(Valid(Double.POSITIVE_INFINITY), validation(Double.POSITIVE_INFINITY))
        assertEquals(
            Valid(Double.NEGATIVE_INFINITY),
            Validation { minimum(Double.NEGATIVE_INFINITY) }(Double.NEGATIVE_INFINITY),
        )

        assertEquals(1, countFieldsWithErrors(validation(9.99999)))
        assertEquals(1, countFieldsWithErrors(validation(9.0)))
        assertEquals(1, countFieldsWithErrors(validation(Double.NEGATIVE_INFINITY)))

        val invalid = validation shouldBeInvalid 9.0
        invalid.errors shouldHaveSize 1
        // Small difference in numbers between kotlin JS and others
        invalid.errors[0].message shouldMatch "must be at least '10(\\.0)?'".toRegex()
    }

    @Test
    fun exclusiveMinimumConstraint() {
        assertEquals(
            Valid<Int>(11),
            Validation<Int> { exclusiveMinimum(10) }(11),
        )

        assertEquals(
            Valid<Long>(11),
            Validation<Long> { exclusiveMinimum(10) }(11),
        )

        assertEquals(
            Valid<Float>(11.0f),
            Validation<Float> { exclusiveMinimum(10) }(11.0f),
        )

        assertEquals(
            Valid<Double>(11.0),
            Validation<Double> { exclusiveMinimum(10) }(11.0),
        )

        assertEquals(
            Valid<String>("b"),
            Validation<String> { exclusiveMinimum("a") }("b"),
        )

        assertEquals(
            Valid<Double>(10.0),
            Validation<Double> { minimum(9) }(10.0),
        )

        val validation = Validation<Double> { exclusiveMinimum(10.0) }

        assertEquals(Valid(11.0), validation(11.0))
        assertEquals(Valid(10.00000001), validation(10.00000001))
        assertEquals(Valid(Double.POSITIVE_INFINITY), validation(Double.POSITIVE_INFINITY))

        assertEquals(1, countFieldsWithErrors(validation(9.0)))
        assertEquals(1, countFieldsWithErrors(validation(9.99999)))
        assertEquals(1, countFieldsWithErrors(validation(-9.0)))
        assertEquals(1, countFieldsWithErrors(validation(Double.NEGATIVE_INFINITY)))
        assertEquals(
            1,
            countFieldsWithErrors(Validation<Double> { exclusiveMinimum(Double.NEGATIVE_INFINITY) }(Double.NEGATIVE_INFINITY)),
        )

        val invalid = validation shouldBeInvalid 9.0
        invalid.errors shouldHaveSize 1
        // Small difference in numbers between kotlin JS and others
        invalid.errors[0].message shouldMatch "must be greater than '10(\\.0)?'".toRegex()
    }

    @Test
    fun minLengthConstraint() {
        val validation = Validation<String> { minLength(10) }

        assertEquals(Valid("HelloWorld"), validation("HelloWorld"))
        assertEquals(Valid("Hello World"), validation("Hello World"))

        assertEquals(1, countFieldsWithErrors(validation("Hello")))
        assertEquals(1, countFieldsWithErrors(validation("")))

        assertEquals("must have at least 10 characters", validation("").get()[0])
    }

    @Test
    fun maxLengthConstraint() {
        val validation = Validation<String> { maxLength(10) }

        assertEquals(Valid("HelloWorld"), validation("HelloWorld"))
        assertEquals(Valid("Hello"), validation("Hello"))
        assertEquals(Valid(""), validation(""))

        assertEquals(1, countFieldsWithErrors(validation("Hello World")))

        assertEquals("must have at most 10 characters", validation("Hello World").get()[0])
    }

    @Test
    fun patternConstraint() {
        val validation = Validation<String> { pattern(".+@.+") }

        validation shouldBeValid "a@a"
        validation shouldBeValid "a@a@a@a"
        validation shouldBeValid " a@a "

        val invalid = validation shouldBeInvalid "a"
        invalid.errors shouldHaveSize 1
        invalid.errors[0].path shouldBe ValidationPath.EMPTY
        invalid.errors[0].message shouldContain "must match pattern '"

        val compiledRegexValidation =
            Validation<String> {
                pattern("^\\w+@\\w+\\.\\w+$".toRegex())
            }

        compiledRegexValidation shouldBeValid "tester@example.com"
        val invalidComplex = (compiledRegexValidation shouldBeInvalid "tester@example")
        invalidComplex.errors shouldHaveSize 1
        invalidComplex.errors[0].message shouldContain "must match pattern '"
        compiledRegexValidation shouldBeInvalid " tester@example.com"
        compiledRegexValidation shouldBeInvalid "tester@example.com "
    }

    @Test
    fun uuidConstraint() {
        val validation = Validation<String> { uuid() }

        assertEquals(Valid("ae40fe0d-05cb-4796-be1f-a1798fec52cf"), validation("ae40fe0d-05cb-4796-be1f-a1798fec52cf"))

        assertEquals(1, countFieldsWithErrors(validation("a")))
        assertEquals("must be a valid UUID string", validation("").get()[0])
    }

    @Test
    fun minSizeConstraint() {
        val validation = Validation<List<String>> { minItems(1) }

        assertEquals(Valid(listOf("a", "b")), validation(listOf("a", "b")))
        assertEquals(Valid(listOf("a")), validation(listOf("a")))

        assertEquals(1, countFieldsWithErrors(validation(emptyList())))

        val arrayValidation = Validation<Array<String>> { minItems(1) }

        arrayOf("a", "b").let { assertEquals(Valid(it), arrayValidation(it)) }
        arrayOf("a").let { assertEquals(Valid(it), arrayValidation(it)) }

        assertEquals(1, countFieldsWithErrors(arrayValidation(emptyArray())))

        val mapValidation = Validation<Map<String, Int>> { minItems(1) }

        assertEquals(Valid(mapOf("a" to 0, "b" to 1)), mapValidation(mapOf("a" to 0, "b" to 1)))
        assertEquals(Valid(mapOf("a" to 0)), mapValidation(mapOf("a" to 0)))

        assertEquals(1, countFieldsWithErrors(mapValidation(emptyMap())))

        assertEquals("must have at least 1 items", validation(emptyList()).get()[0])
    }

    @Test
    fun maxSizeConstraint() {
        val validation = Validation<List<String>> { maxItems(1) }

        assertEquals(Valid(emptyList()), validation(emptyList()))
        assertEquals(Valid(listOf("a")), validation(listOf("a")))

        assertEquals(1, countFieldsWithErrors(validation(listOf("a", "b"))))

        val arrayValidation = Validation<Array<String>> { maxItems(1) }

        emptyArray<String>().let { assertEquals(Valid(it), arrayValidation(it)) }
        arrayOf("a").let { assertEquals(Valid(it), arrayValidation(it)) }

        assertEquals(1, countFieldsWithErrors(arrayValidation(arrayOf("a", "b"))))

        val mapValidation = Validation<Map<String, Int>> { maxItems(1) }

        assertEquals(Valid(emptyMap()), mapValidation(emptyMap()))
        assertEquals(Valid(mapOf("a" to 0)), mapValidation(mapOf("a" to 0)))

        assertEquals(1, countFieldsWithErrors(mapValidation(mapOf("a" to 0, "b" to 1))))

        assertEquals("must have at most 1 items", mapValidation(mapOf("a" to 0, "b" to 1)).get()[0])
    }

    @Test
    fun minPropertiesConstraint() {
        val validation = Validation<Map<String, Int>> { minProperties(1) }

        assertEquals(Valid(mapOf("a" to 0, "b" to 1)), validation(mapOf("a" to 0, "b" to 1)))
        assertEquals(Valid(mapOf("a" to 0)), validation(mapOf("a" to 0)))

        assertEquals(1, countFieldsWithErrors(validation(emptyMap())))

        assertEquals("must have at least 1 properties", validation(emptyMap()).get()[0])
    }

    @Test
    fun maxPropertiesConstraint() {
        val validation = Validation<Map<String, Int>> { maxProperties(1) }

        assertEquals(Valid(emptyMap()), validation(emptyMap()))
        assertEquals(Valid(mapOf("a" to 0)), validation(mapOf("a" to 0)))

        assertEquals(1, countFieldsWithErrors(validation(mapOf("a" to 0, "b" to 1))))

        assertEquals("must have at most 1 properties", validation(mapOf("a" to 0, "b" to 1)).get()[0])
    }

    @Test
    fun uniqueItemsConstraint() {
        val validation = Validation<List<String>> { uniqueItems(true) }

        assertEquals(Valid(emptyList()), validation(emptyList()))
        assertEquals(Valid(listOf("a")), validation(listOf("a")))
        assertEquals(Valid(listOf("a", "b")), validation(listOf("a", "b")))

        assertEquals(1, countFieldsWithErrors(validation(listOf("a", "a"))))

        val arrayValidation = Validation<Array<String>> { uniqueItems(true) }

        emptyArray<String>().let { assertEquals(Valid(it), arrayValidation(it)) }
        arrayOf("a").let { assertEquals(Valid(it), arrayValidation(it)) }
        arrayOf("a", "b").let { assertEquals(Valid(it), arrayValidation(it)) }

        assertEquals(1, countFieldsWithErrors(arrayValidation(arrayOf("a", "a"))))

        assertEquals("all items must be unique", validation(listOf("a", "a")).get()[0])
    }
}
