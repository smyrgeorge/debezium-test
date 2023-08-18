package io.smyrgeorge.test.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class TestService {

    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    suspend fun test() {
        log.info("Hola!")
    }
}