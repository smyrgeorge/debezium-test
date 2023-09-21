package io.smyrgeorge.test.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
class Props(
    val app: App
) {
    @Configuration
    @ConfigurationProperties("app")
    class App {
        lateinit var env: String
    }
}