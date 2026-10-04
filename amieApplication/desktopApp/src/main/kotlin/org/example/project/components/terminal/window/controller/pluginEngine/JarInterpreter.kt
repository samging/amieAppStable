package org.example.project.components.terminal.window.controller.pluginEngine

import java.io.File
import java.net.URLClassLoader

class JarInterpreter {
    fun executeJarFunction(jarPath: String, className: String, methodName: String): Any? {
        val jarFile = File(jarPath)

        // Create a ClassLoader pointing to the external JAR
        val classLoader = URLClassLoader(
            arrayOf(jarFile.toURI().toURL()),
            Thread.currentThread().contextClassLoader
        )

        // Load the generated class (e.g., "org.example.MainKt")
        val clazz = classLoader.loadClass(className)

        // Find the static top-level function/method
        val method = clazz.getMethod(methodName)

        // Invoke the function (null receiver because top-level Kotlin functions compile to static methods)
        return method.invoke(null)
    }

    fun interpret(): Any? {
        val result = executeJarFunction(
            jarPath = "/Users/samuel/Documents/plugJar/build/libs/plugJar-1.0-SNAPSHOT-all.jar",
            className = "org.example.MainKt",
            methodName = "run"
        )
        println("Execution Result: $result")
        return result
    }
}