package io.smyrgeorge.test

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SpringReactiveKotlin

fun main(args: Array<String>) {
    runApplication<SpringReactiveKotlin>(*args)
}
