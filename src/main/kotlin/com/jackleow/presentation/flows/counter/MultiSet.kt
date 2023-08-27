package com.jackleow.presentation.flows.counter

class MultiSet<out E> internal constructor(
    internal val countsByElement: Map<@UnsafeVariance E, Int>, val elementsByCount: Map<Int, List<E>>
) {
    constructor() : this(mapOf(), mapOf())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MultiSet<*>

        if (elementsByCount != other.elementsByCount) return false

        return true
    }

    override fun hashCode(): Int {
        return elementsByCount.hashCode()
    }

    override fun toString(): String = "MultiSet($elementsByCount)"
}

operator fun <E> MultiSet<E>.plus(element: E): MultiSet<E> =
    when (val oldCount: Int = this.countsByElement[element] ?: 0) {
        Int.MAX_VALUE -> this

        else -> {
            val newCount: Int = oldCount + 1

            val newCountElems: List<E> = (elementsByCount[newCount] ?: listOf()) + element
            val oldCountElems: List<E> = (elementsByCount[oldCount] ?: listOf()) - element
            MultiSet(
                countsByElement + (element to newCount),
                (if (oldCountElems.isEmpty()) elementsByCount - oldCount
                else elementsByCount + (oldCount to oldCountElems)) + (newCount to newCountElems)
            )
        }
    }

operator fun <E> MultiSet<E>.minus(element: E): MultiSet<E> =
    when (val oldCount: Int? = this.countsByElement[element]) {
        null -> this

        else -> {
            val newCount: Int = oldCount - 1

            val newCountElems: List<E> = elementsByCount[newCount] ?: listOf()
            val oldCountElems: List<E> = (elementsByCount[oldCount] ?: listOf()) - element
            val elemsByCountOldCountUpdated: Map<Int, List<E>> =
                if (oldCountElems.isEmpty()) elementsByCount - oldCount
                else elementsByCount + (oldCount to oldCountElems)
            MultiSet(
                if (newCount == 0) countsByElement - element
                else countsByElement + (element to newCount),
                if (newCount == 0) elemsByCountOldCountUpdated
                else elemsByCountOldCountUpdated + (newCount to listOf(element) + newCountElems)
            )
        }
    }

fun <E>multiSetOf(vararg elements: E): MultiSet<E> =
    elements.fold(MultiSet()) { accum: MultiSet<E>, element: E ->
        accum + element
    }
