package com.jackleow.presentation.flows.counter

import com.jackleow.presentation.KotestProjectConfig
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.positiveInt
import io.kotest.property.checkAll

@OptIn(ExperimentalKotest::class)
class FifoBoundedSetProp : WordSpec({
    concurrency = KotestProjectConfig.parallelism

    "A FifoBoundedSet" should {
        "never contain more elements than maxSize" {
            checkAll(
                Arb.positiveInt(100), Arb.list(Arb.int())
            ) { maxSize: Int, elements: List<Int> ->
                // Set up & Test
                val (instance, _) = FifoBoundedSet<Int>(maxSize).addAll(elements)

                // Verify
                instance.insertionOrder.size shouldBeLessThanOrEqual maxSize
            }
        }

        "always include the most recently added elements" {
            checkAll(
                Arb.positiveInt(100), Arb.list(Arb.int())
            ) { maxSize: Int, elements: List<Int> ->
                // Set up & Test
                val (instance, _) = FifoBoundedSet<Int>(maxSize).addAll(elements)

                // Verify
                instance.insertionOrder.toSet() shouldContainAll elements.takeLast(maxSize).toSet()
            }
        }

        "only evict the least recently added elements" {
            checkAll(
                Arb.positiveInt(100), Arb.list(Arb.int())
            ) { maxSize: Int, elements: List<Int> ->
                // Set up & Test
                val (_, actualEffects: List<FifoBoundedSet.Effect<Int>>) =
                    FifoBoundedSet<Int>(maxSize).addAll(elements)
                val actualEvictions: Set<Int> = actualEffects
                    .mapNotNull {
                        when (it) {
                            is FifoBoundedSet.Added -> null
                            is FifoBoundedSet.AddedEvicting -> it.value
                            is FifoBoundedSet.NotAdded -> null
                        }
                    }.toSet()

                // Verify
                elements.dropLast(maxSize).toSet() shouldContainAll actualEvictions
            }
        }

        "never evict when not full" {
            checkAll(
                Arb.list(Arb.int(), 1..100)
            ) { elements: List<Int> ->
                // Set up & Test
                val (instance, actualEffects: List<FifoBoundedSet.Effect<Int>>) =
                    FifoBoundedSet<Int>(elements.size).addAll(elements)

                // Verify
                val actualEvictions: List<FifoBoundedSet.Effect<Int>> =
                    actualEffects.filterIsInstance<FifoBoundedSet.AddedEvicting<Int>>()
                instance.insertionOrder.toSet() shouldBe elements.toSet()
                actualEvictions.shouldBeEmpty()
            }
        }

        // add/addAll Equivalence
        "add and addAll are equivalent given identical input" {
            checkAll(
                Arb.positiveInt(100), Arb.list(Arb.int())
            ) { maxSize: Int, elements: List<Int> ->
                // Set up
                val empty = FifoBoundedSet<Int>(maxSize)

                // Test
                val (instanceUsingAddAll: FifoBoundedSet<Int>, _) = empty.addAll(elements)
                val instanceUsingAdd: FifoBoundedSet<Int> =
                    elements.fold(empty) { accum: FifoBoundedSet<Int>, elem: Int ->
                        val (accumNext: FifoBoundedSet<Int>, _) = accum.add(elem)

                        accumNext
                    }

                // Verify
                instanceUsingAddAll shouldBe instanceUsingAdd
            }
        }

        "add and addAll produces identical effects given identical input" {
            checkAll(
                Arb.positiveInt(100), Arb.list(Arb.int())
            ) { maxSize: Int, elements: List<Int> ->
                // Set up
                val empty = FifoBoundedSet<Int>(maxSize)

                // Test
                val (_, actualEffectsAddAll: List<FifoBoundedSet.Effect<Int>>) = empty.addAll(elements)
                val (_, actualEffectsAdd: List<FifoBoundedSet.Effect<Int>>) =
                    elements.fold(
                        Pair(empty, listOf())
                    ) { accumAndEffects: Pair<FifoBoundedSet<Int>, List<FifoBoundedSet.Effect<Int>>>, elem: Int ->

                        val (accum: FifoBoundedSet<Int>, effects: List<FifoBoundedSet.Effect<Int>>) = accumAndEffects
                        val (accumNext: FifoBoundedSet<Int>, effect: FifoBoundedSet.Effect<Int>) = accum.add(elem)

                        Pair(accumNext, effects + effect)
                    }

                // Verify
                actualEffectsAddAll shouldBe actualEffectsAdd
            }
        }
    }
})
