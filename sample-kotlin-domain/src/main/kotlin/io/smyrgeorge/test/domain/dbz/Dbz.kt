package io.smyrgeorge.test.domain.dbz

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Dbz(val topic: String)