package io.smyrgeorge.test.api.rest

import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse

@Component
class RouteHandler(
    private val testHandler: TestHandler
) {

    @Bean
    fun routes(): RouterFunction<ServerResponse> =
        testHandler.routes()

}