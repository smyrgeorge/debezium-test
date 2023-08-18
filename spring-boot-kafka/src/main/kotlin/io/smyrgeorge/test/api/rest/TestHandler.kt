package io.smyrgeorge.test.api.rest

import io.smyrgeorge.test.config.Constants
import io.smyrgeorge.test.service.TestService
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.coRouter

@Component
class TestHandler(
    val service: TestService
) {

    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    private fun String.uri(): String =
        "${Constants.API_V1_BASE_URL}/test"

    fun routes(): RouterFunction<ServerResponse> = coRouter {
        GET("".uri()) { test() }
    }

    private suspend fun test(): ServerResponse {
        service.test()
        return ServerResponse.ok().build().awaitSingle()
    }
}