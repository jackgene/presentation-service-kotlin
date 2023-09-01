package com.jackleow.presentation.flows.counter

class FifoBoundedSet<E>(
    private val maxSize: Int,
    private val uniques: Set<E> = setOf(),
    val insertionOrder: List<E> = listOf()
) {
    sealed interface Effect<out E>
    data class Added<E>(val added: E) : Effect<E>
    data class AddedEvicting<E>(val added: E, val evicting: E) : Effect<E>

    init {
        require(maxSize >= 1) { "maxSize must be positive" }
    }

    private fun copy(
        newUniques: Set<E> = uniques, newInsertionOrder: List<E> = insertionOrder
    ): FifoBoundedSet<E> = FifoBoundedSet(
        maxSize, newUniques, newInsertionOrder
    )

    fun add(element: E): Pair<FifoBoundedSet<E>, Effect<E>?> =
        if (uniques.contains(element))
            if (element == insertionOrder.last()) Pair(this, null)
            else Pair(copy(newInsertionOrder = insertionOrder - element + element), null)
        else
            if (uniques.size < maxSize) Pair(
                copy(
                    newUniques = uniques + element,
                    newInsertionOrder = insertionOrder + element
                ),
                Added(element)
            )
            else Pair(
                copy(
                    newUniques = uniques + element - insertionOrder.first(),
                    newInsertionOrder = insertionOrder.drop(1) + element
                ),
                AddedEvicting(element, insertionOrder.first())
            )

    fun addAll(elements: List<E>): Pair<FifoBoundedSet<E>, List<Effect<E>>> = elements
        .fold(Pair(this, listOf())) { accum: Pair<FifoBoundedSet<E>, List<Effect<E>>>, element: E ->
            val (accumSet: FifoBoundedSet<E>, accumEffects: List<Effect<E>>) = accum
            val (nextAccumSet: FifoBoundedSet<E>, nextEffect: Effect<E>?) = accumSet.add(element)

            Pair(
                nextAccumSet, accumEffects + listOfNotNull(nextEffect)
            )
        }
        .let { (set: FifoBoundedSet<E>, effects: List<Effect<E>>) ->
            Pair(
                set,
                effects.takeLast(maxSize).map {
                    when (it) {
                        is AddedEvicting ->
                            if (uniques.contains(it.evicting)) it
                            else Added(it.added)

                        else -> it
                    }
                }
            )
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FifoBoundedSet<*>

        if (maxSize != other.maxSize) return false
        if (insertionOrder != other.insertionOrder) return false

        return true
    }

    override fun hashCode(): Int {
        var result = maxSize
        result = 31 * result + insertionOrder.hashCode()
        return result
    }

    override fun toString(): String =
        insertionOrder.joinToString(", ", "FifoBoundedSet(", ")")
}
