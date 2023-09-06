package com.jackleow.presentation

import io.kotest.core.config.AbstractProjectConfig

object KotestProjectConfig : AbstractProjectConfig() {
    init {
        displayFullTestPath = true
    }

    override val parallelism = run {
        val par = Runtime.getRuntime().availableProcessors()
        println("Parallelism: $par")

        par
    }
}
