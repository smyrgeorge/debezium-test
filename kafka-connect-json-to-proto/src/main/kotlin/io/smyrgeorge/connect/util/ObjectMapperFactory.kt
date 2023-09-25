package io.smyrgeorge.connect.util

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

object ObjectMapperFactory {

    fun createSnakeCase(): ObjectMapper = create(PropertyNamingStrategies.SNAKE_CASE)
    fun createCamelCase(): ObjectMapper = create(PropertyNamingStrategies.LOWER_CAMEL_CASE)

    private fun create(propertyNamingStrategy: PropertyNamingStrategy): ObjectMapper =
        ObjectMapper().apply {
            registerKotlinModule()
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
            disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
            enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)  // Timestamps as milliseconds
            this.propertyNamingStrategy = propertyNamingStrategy
            setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY)
            setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        }

}