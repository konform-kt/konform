package io.konform.validation.string

import io.konform.validation.Constraint
import io.konform.validation.ValidationBuilder

public fun ValidationBuilder<String>.notBlank(): Constraint<String> = addConstraint("must not be blank") { it.isNotBlank() }

/**
 * Checks that the string contains a match with the given [Regex].
 * */
public fun ValidationBuilder<String>.containsPattern(pattern: Regex): Constraint<String> =
    addConstraint("must include regex '$pattern'") {
        it.contains(pattern)
    }

public fun ValidationBuilder<String>.containsPattern(pattern: String): Constraint<String> = containsPattern(pattern.toRegex())
