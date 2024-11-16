package io.konform.validation.constraints

import io.konform.validation.Constraint
import io.konform.validation.ValidationBuilder

public fun ValidationBuilder<String>.notBlank(): Constraint<String> = constrain("must not be blank") { it.isNotBlank() }

public fun ValidationBuilder<String>.minLength(length: Int): Constraint<String> {
    require(length >= 0) { IllegalArgumentException("minLength requires the length to be >= 0") }
    return constrain("must have at least $length characters") { it.length >= length }
}

public fun ValidationBuilder<String>.maxLength(length: Int): Constraint<String> {
    require(length >= 0) { IllegalArgumentException("maxLength requires the length to be >= 0") }
    return constrain("must have at most $length characters") { it.length <= length }
}

/** Enforces the string must be UUID hex format. */
public fun ValidationBuilder<String>.uuid(): Constraint<String> =
    pattern("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$") hint "must be a valid UUID string"

public fun ValidationBuilder<String>.pattern(pattern: String): Constraint<String> = pattern(pattern.toRegex())

public fun ValidationBuilder<String>.pattern(pattern: Regex): Constraint<String> =
    constrain("must match pattern '$pattern'") { it.matches(pattern) }

/**
 * Checks that the string contains a match with the given [Regex].
 * */
public fun ValidationBuilder<String>.containsPattern(pattern: Regex): Constraint<String> =
    constrain("must include pattern '$pattern'") {
        it.contains(pattern)
    }

public fun ValidationBuilder<String>.containsPattern(pattern: String): Constraint<String> = containsPattern(pattern.toRegex())
