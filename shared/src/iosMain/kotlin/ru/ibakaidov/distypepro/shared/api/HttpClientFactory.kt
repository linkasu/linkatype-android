package ru.ibakaidov.distypepro.shared.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal actual fun createDefaultHttpClient(jsonConfig: Json): HttpClient {
    return HttpClient {
        install(ContentNegotiation) {
            json(jsonConfig)
        }
    }
}
