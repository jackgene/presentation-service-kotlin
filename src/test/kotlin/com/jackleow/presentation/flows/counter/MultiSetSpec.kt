package com.jackleow.presentation.flows.counter

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

class MultiSetSpec : WordSpec({
    "A MultiSet" should {
        val empty: MultiSet<String> = MultiSet()

        "be initially empty" {
            // Set up & Test
            val instance: MultiSet<String> = empty

            // Verify
            instance.countsByElement.shouldBeEmpty()
            instance.elementsByCount.shouldBeEmpty()
        }

        "record correct counts" {
            // Set up & Test
            val instance: MultiSet<String> = empty -
                    // "test-1"
                    "test-1" + // 0
                    "test-1" + // 1
                    "test-1" - // 2
                    "test-1" + // 1
                    "test-1" + // 2
                    "test-1" + // 3
                    "test-1" + // 4
                    "test-1" - // 5
                    "test-1" - // 4
                    "test-1" - // 3
                    "test-1" - // 2
                    "test-1" + // 1
                    // "test-2"
                    "test-2" + // 1
                    "test-2" + // 2
                    "test-2" + // 3
                    "test-2" + // 4
                    "test-2"   // 5

            // Verify
            instance.countsByElement shouldBe mapOf("test-1" to 1, "test-2" to 5)
            instance.elementsByCount shouldBe mapOf(1 to listOf("test-1"), 5 to listOf("test-2"))
        }

        "never record counts less than zero" {
            // Set up
            val instanceSetup: MultiSet<String> = empty

            // Test
            val instance: MultiSet<String> = instanceSetup - "test"

            // Verify
            instance.countsByElement.shouldBeEmpty()
            instance.elementsByCount.shouldBeEmpty()
            instance shouldBeSameInstanceAs instanceSetup
        }

        "never record counts greater than the maximum" {
            // Set up
            val instanceSetup: MultiSet<String> = MultiSet(
                countsByElement = mapOf("test" to Int.MAX_VALUE),
                elementsByCount = mapOf(Int.MAX_VALUE to listOf("test"))
            )

            // Test
            val instance: MultiSet<String> = instanceSetup + "test" // Should not overflow

            // Verify
            instance.countsByElement shouldBe mapOf("test" to Int.MAX_VALUE)
            instance.elementsByCount shouldBe mapOf(Int.MAX_VALUE to listOf("test"))
            instance shouldBeSameInstanceAs instanceSetup
        }

        "append element to elementsByCount when incremented" {
            // Set up & Test
            val instance: MultiSet<String> = empty + "test-1" + "test-2"

            // Verify
            // Incremented value should be appended
            instance.elementsByCount shouldBe mapOf(1 to listOf("test-1", "test-2"))
        }

        "prepend element to elementsByCount when decremented" {
            // Set up
            val instanceSetup: MultiSet<String> = empty + "test-1" + "test-2" + "test-2"

            // Set up & Test
            val instance: MultiSet<String> = instanceSetup - "test-2" // Decrement from 2 -> 1

            // Verify
            // Incremented value should be appended
            instance.elementsByCount shouldBe mapOf(1 to listOf("test-2", "test-1"))
        }

        "omit zero counts" {
            // Set up & Test
            val instance: MultiSet<String> = empty - "test" - "test"

            // Verify
            instance.countsByElement.shouldBeEmpty()
            instance.elementsByCount.shouldBeEmpty()
        }
    }
})
