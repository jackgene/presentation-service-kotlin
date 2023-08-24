package com.jackleow.presentation

import com.jackleow.presentation.plugins.*
import com.jackleow.presentation.services.PresentationService
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.cio.EngineMain.main(arrayOf("-P:presentation.htmlPath=${args[0]}"))
}

fun Application.module() {
    val service = PresentationService(environment.config)

    configureSerialization()
    configureSockets(service)
    configureRouting(service)
}
