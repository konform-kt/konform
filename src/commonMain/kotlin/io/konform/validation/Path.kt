package io.konform.validation

import kotlin.reflect.KProperty1

data class Path(private val l: List<PathNode>) : List<PathNode> by l {
    constructor(vararg nodes: PathNode) : this(nodes.toList())

    override fun toString(): String {
        return "." + l.joinToString(".")
    }

    operator fun plus(node: PathNode) = Path(l + node)

    operator fun plus(other: Path) = Path(l + other.l)
}

sealed class PathNode

open class Property<T>(val property: KProperty1<T, Any?>) : PathNode() {
    override fun toString(): String = property.name

    override fun equals(other: Any?) = property == (other as? Property<*>)?.property

    override fun hashCode() = property.hashCode()

    operator fun plus(index: Index) = PropertyElement(this, index)

    operator fun plus(key: Key) = PropertyElement(this, key)
}

class PropertyElement<T> private constructor(val property: Property<T>, private val accessor: String) : PathNode() {
    public constructor(property: Property<T>, index: Index) : this(property, "$index")
    public constructor(property: Property<T>, key: Key) : this(property, "$key")

    override fun toString(): String = "$property$accessor"

    override fun equals(other: Any?) =
        property == (other as? PropertyElement<*>)?.property && accessor == other.accessor

    override fun hashCode() = property.hashCode() * 31 + accessor.hashCode()
}

data class Index(val index: Int) : PathNode() {
    override fun toString(): String = "[$index]"
}

data class Key(val key: String) : PathNode() {
    override fun toString(): String = "[$key]"
}
