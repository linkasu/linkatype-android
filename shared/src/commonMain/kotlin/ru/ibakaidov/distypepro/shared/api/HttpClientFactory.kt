package ru.ibakaidov.distypepro.shared.api

import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json

internal expect fun createDefaultHttpClient(jsonConfig: Json): HttpClient
