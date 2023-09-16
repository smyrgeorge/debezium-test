package io.smyrgeorge.test.util

import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder


class ClassLoader(packageName: String) {

    private val configuration = ConfigurationBuilder()
        .setUrls(ClasspathHelper.forPackage(packageName))
        .addScanners(Scanners.TypesAnnotated)

    private val reflections = Reflections(configuration)

    fun getTypesAnnotatedWith(annotation: Class<out Annotation>): Set<Class<*>> =
        reflections.getTypesAnnotatedWith(annotation)
}