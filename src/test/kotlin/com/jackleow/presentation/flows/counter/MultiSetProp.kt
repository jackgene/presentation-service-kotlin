package com.jackleow.presentation.flows.counter

import com.jackleow.presentation.KotestProjectConfig
import io.kotest.assertions.fail
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.maps.shouldHaveKey
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll

@OptIn(ExperimentalKotest::class)
class MultiSetProp : WordSpec({
    concurrency = KotestProjectConfig.parallelism

    val duplicativeElements: Gen<List<String>> =
        Arb
            .list(
                Arb.pair(Arb.stringPattern("[a-z]+"), Arb.positiveInt(10)),
                1..100
            )
            .map { elementsAndCounts: List<Pair<String, Int>> ->
                elementsAndCounts
                    .flatMap { (element, count) ->
                        (1..count).map { element }
                    }
                    .shuffled()
            }

    "A MultiSet" should {
        "counts by element and elements by count reciprocate" {
            checkAll(
                duplicativeElements, duplicativeElements
            ) { increments: List<String>, decrements: List<String> ->
                // Test
                val instance: MultiSet<String> = decrements.fold(
                    increments.fold(MultiSet()) { accum: MultiSet<String>, increment: String ->
                        accum + increment
                    }
                ) { accum: MultiSet<String>, decrement: String ->
                    accum - decrement
                }

                // Verify
                for ((element: String, count: Int) in instance.countsByElement) {
                    instance.elementsByCount shouldHaveKey count
                    instance.elementsByCount[count]?.shouldContain(element)
                }
                for ((count: Int, elements: List<String>) in instance.elementsByCount) {
                    for (element: String in elements) {
                        instance.countsByElement[element] shouldBe count
                    }
                }
            }
        }

        "never record zero counts" {
            checkAll(
                duplicativeElements, duplicativeElements
            ) { increments: List<String>, decrements: List<String> ->
                // Test
                val instance: MultiSet<String> = decrements.fold(
                    increments.fold(MultiSet()) { accum: MultiSet<String>, increment: String ->
                        accum + increment
                    }
                ) { accum: MultiSet<String>, decrement: String ->
                    accum - decrement
                }

                // Verify
                for (count: Int in instance.countsByElement.values) {
                    count shouldBeGreaterThan 0
                }
                for (count: Int in instance.elementsByCount.keys) {
                    count shouldBeGreaterThan 0
                }
            }
        }

        "most recently incremented element is the last of elements by count" {
            checkAll(duplicativeElements) { elements: List<String> ->
                elements.fold(MultiSet()) { accum: MultiSet<String>, element: String ->
                    val nextAccum: MultiSet<String> = accum + element
                    val count: Int = nextAccum.countsByElement[element] ?: fail("countsByElement should have $element")
                    val elemsForCount: List<String> =
                        nextAccum.elementsByCount[count] ?: fail("elementsByCount should have $count")
                    elemsForCount.last() shouldBe element

                    nextAccum
                }
            }
        }

        "most recently decremented element is the first of elements by count" {
            checkAll(duplicativeElements) { elements: List<String> ->
                val instance: MultiSet<String> =
                    elements.fold(MultiSet()) { accum: MultiSet<String>, element: String ->
                        accum + element
                    }

                elements.fold(instance) { accum: MultiSet<String>, element: String ->
                    val nextAccum: MultiSet<String> = accum - element
                    nextAccum.countsByElement[element]?.let { count: Int -> // Can be missing if decremented to 0
                        val elemsForCount: List<String> =
                            nextAccum.elementsByCount[count] ?: fail("elementsByCount should have $count")
                        elemsForCount.first() shouldBe element
                    }

                    nextAccum
                }
            }
        }
    }
})
