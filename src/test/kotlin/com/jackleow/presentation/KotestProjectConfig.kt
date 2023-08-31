package com.jackleow.presentation

import io.kotest.core.config.AbstractProjectConfig
import java.lang.Integer.max

object KotestProjectConfig : AbstractProjectConfig() {
    init {
        displayFullTestPath = true
    }

    override val parallelism = max(2, Runtime.getRuntime().availableProcessors() * 4 / 3)
}
