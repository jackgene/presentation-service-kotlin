package com.jackleow.presentation.flows.counter

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

class FifoBoundedSetSpec : WordSpec({
    "A FifoBoundedSet of size 2" `when` {
        val empty: FifoBoundedSet<String> = FifoBoundedSet(2)

        "no element has been added" should {
            // Set up
            val instance: FifoBoundedSet<String> = empty

            "be empty" {
                // Verify
                instance.insertionOrder.shouldBeEmpty()
            }

            "accept 1 new element without evicting" {
                // Test
                val (actualUpdatedInstance, actualEffect) = instance.add("test")

                // Verify
                actualEffect shouldBe FifoBoundedSet.Added("test")
                actualUpdatedInstance.insertionOrder shouldBe listOf("test")
            }

            "accept 2 new elements without evicting" {
                // Test
                val (actualUpdatedInstance, actualEffects) = instance.addAll(listOf("test-1", "test-2"))

                // Verify
                actualEffects shouldBe listOf(
                    FifoBoundedSet.Added("test-1"),
                    FifoBoundedSet.Added("test-2")
                )
                actualUpdatedInstance.insertionOrder shouldBe listOf("test-1", "test-2")
            }

            "accept 3 new elements ignoring the first" {
                // Test
                val (actualUpdatedInstance, actualEffects) = instance.addAll(listOf("test-1", "test-2", "test-3"))

                // Verify
                actualEffects shouldBe listOf(
                    FifoBoundedSet.Added("test-2"),
                    FifoBoundedSet.Added("test-3")
                )
                actualUpdatedInstance.insertionOrder shouldBe listOf("test-2", "test-3")
            }
        }

        "1 element has been added" should {
            // Set up
            val (instance: FifoBoundedSet<String>, _) = empty.add("test-1")

            "contain the element" {
                // Verify
                instance.insertionOrder shouldBe listOf("test-1")
            }

            "accept 1 new element without evicting" {
                // Test
                val (actualUpdatedInstance, actualEffect) = instance.add("test-2")

                // Verify
                actualEffect shouldBe FifoBoundedSet.Added("test-2")
                actualUpdatedInstance.insertionOrder shouldBe listOf("test-1", "test-2")
            }

            "not accept the existing element again" {
                // Test
                val (actualUpdatedInstance, actualEffect) = instance.add("test-1")

                // Verify
                actualEffect shouldBe null
                actualUpdatedInstance.insertionOrder shouldBe listOf("test-1")
            }

            "accept 2 new elements evicting the existing element" {
                // Test
                val (actualUpdatedInstance, actualEffects) = instance.addAll(listOf("test-2", "test-3"))

                // Verify
                actualEffects shouldBe listOf(
                    FifoBoundedSet.Added("test-2"),
                    FifoBoundedSet.AddedEvicting("test-3", "test-1")
                )
                actualUpdatedInstance.insertionOrder shouldBe listOf("test-2", "test-3")
            }
        }

        "2 elements have been added" should {
            // Set up
            val (instance: FifoBoundedSet<String>, _) = empty.addAll(listOf("test-1", "test-2"))

            "contain the elements" {
                // Verify
                instance.insertionOrder shouldBe listOf("test-1", "test-2")
            }

            "accept 1 new element evicting the first existing element" {
                // Test
                val (actualUpdatedInstance, actualEffect) = instance.add("test-3")

                // Verify
                actualEffect shouldBe FifoBoundedSet.AddedEvicting("test-3", "test-1")
                actualUpdatedInstance.insertionOrder shouldBe listOf("test-2", "test-3")
            }

            "not accept the existing element again, but update its insertion order" {
                // Test
                val (actualUpdatedInstance, actualEffect) = instance.add("test-1")

                // Verify
                actualEffect shouldBe null
                actualUpdatedInstance.insertionOrder shouldBe listOf("test-2", "test-1")
            }

            "accept 2 new elements evicting all existing elements" {
                // Test
                val (actualUpdatedInstance, actualEffects) = instance.addAll(listOf("test-3", "test-4"))

                // Verify
                actualEffects shouldBe listOf(
                    FifoBoundedSet.AddedEvicting("test-3", "test-1"),
                    FifoBoundedSet.AddedEvicting("test-4", "test-2")
                )
                actualUpdatedInstance.insertionOrder shouldBe listOf("test-3", "test-4")
            }

            "not accept 2 existing elements, but update their insertion order" {
                // Test
                val (actualUpdatedInstance, actualEffects) = instance.addAll(listOf("test-2", "test-1"))

                // Verify
                actualEffects shouldBe listOf()
                actualUpdatedInstance.insertionOrder shouldBe listOf("test-2", "test-1")
            }

            "accept a new element, but not an existing element, updating the insertion order of the existing element" {
                // Test
                val (actualUpdatedInstance, actualEffects) = instance.addAll(
                    listOf(
                        "test-1", // test-1 not added, but is no longer first-in
                        "test-3"  // test-2 is evicted instead
                    )
                )

                // Verify
                actualEffects shouldBe listOf(FifoBoundedSet.AddedEvicting("test-3", "test-2"))
                actualUpdatedInstance.insertionOrder shouldBe listOf("test-1", "test-3")
            }
        }
    }
})
