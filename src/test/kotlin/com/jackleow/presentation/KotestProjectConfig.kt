package com.jackleow.presentation

import io.kotest.core.config.AbstractProjectConfig

object KotestProjectConfig : AbstractProjectConfig() {
    init {
        displayFullTestPath = true
    }

    override val parallelism = Runtime.getRuntime().availableProcessors()
}
