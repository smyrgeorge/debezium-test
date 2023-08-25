package io.smyrgeorge.test

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SpringReactiveKotlinAvro

fun main(args: Array<String>) {
    runApplication<SpringReactiveKotlinAvro>(*args)
}
