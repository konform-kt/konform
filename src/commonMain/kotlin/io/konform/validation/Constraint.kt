package io.konform.validation

public class Constraint<in R> internal constructor(
    public val hint: String,
    public val templateValues: List<String>,
    public val test: (R) -> Boolean,
)
