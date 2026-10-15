package org.example.project.components.terminal.window.controller.pluginEngine

import org.example.project.getDirectory
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import javax.tools.ToolProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * A package jar built here, with metadata inside it: its config template lands in the config folder, the
 * package finds the folder through configure(File), the resource root and the system property, and the tests
 * it declares run and are reported.
 */
class PackageMetaTest {
    private val pluginDir = File(getDirectory().toString(), "pluginDir").apply { mkdirs() }

    private val source = """
        package t;
        import java.util.*; import java.io.*; import java.nio.file.*;
        public class Cfg {
            private String greeting = "none";
            public void configure(File dir) { try { greeting = Files.readString(new File(dir, "greeting.txt").toPath()).trim(); } catch (IOException e) { greeting = "none"; } }
            public List<String> run(String text) {
                String prop = System.getProperty("amie.package.config") == null ? "no property" : "property set";
                String resource = getClass().getClassLoader().getResource("greeting.txt") == null ? "no resource" : "resource found";
                if (text.equals("boom")) throw new IllegalStateException("asked to fail");
                return Arrays.asList(greeting + ": " + text, prop, resource);
            }
            public void close() {}
        }
    """.trimIndent()

    private fun buildJar(name: String, meta: String?): File {
        val work = File(pluginDir, "build-$name").apply { deleteRecursively(); mkdirs() }
        val src = File(work, "t/Cfg.java").apply { parentFile.mkdirs(); writeText(source) }
        val compiler = ToolProvider.getSystemJavaCompiler() ?: error("no compiler in this JDK")
        assertEquals(0, compiler.run(null, null, null, "-d", work.path, src.path))
        val jar = File(pluginDir, name)
        JarOutputStream(jar.outputStream()).use { out ->
            out.putNextEntry(JarEntry("t/Cfg.class")); out.write(File(work, "t/Cfg.class").readBytes()); out.closeEntry()
            if (meta != null) { out.putNextEntry(JarEntry(PackageMetadata.IN_JAR)); out.write(meta.toByteArray()); out.closeEntry() }
        }
        work.deleteRecursively()
        return jar
    }

    private val meta = """
        {"name": "cfg", "version": "1.2", "description": "a package that reads a greeting from its config folder",
         "config": [{"file": "greeting.txt", "required": true, "description": "the word before the text", "template": "hi\n"}],
         "env": [{"name": "CFG_TEST_UNSET_VAR", "required": false}],
         "tests": [{"name": "greets", "input": "there", "contains": "hi: there", "lines": 3},
                   {"name": "sees its config", "input": "x", "contains": "property set"},
                   {"name": "resource root", "input": "y", "contains": "resource found"}]}
    """.trimIndent()

    @Test
    fun `the config template is written, the package is configured, and the declared tests pass`() {
        val name = "cfgtest-${System.nanoTime()}.jar"
        val jar = buildJar(name, meta)
        PackageMetadata.configDir(name).deleteRecursively()
        val read = PackageMetadata.read(jar)
        assertNotNull(read)
        assertEquals("1.2", read.version)
        assertEquals(3, read.tests.size)

        val session = JarPackages.open(name)
        try {
            assertTrue(File(PackageMetadata.configDir(name), "greeting.txt").isFile, "the template is written on open")
            assertEquals(listOf("hi: there", "property set", "resource found"), session.inference("there"))
            assertEquals(PackageMetadata.configDir(name).absolutePath, (session.getStatus() as Map<*, *>)["configDir"])
        } finally {
            session.close()
        }

        val report = JarPackages.test(name)
        assertTrue(report.ok, report.summary() + " " + report.results.joinToString { "${it.name}: ${it.reason}" })
        assertEquals(3, report.passed)
        assertFalse(report.smoke)
        assertNotNull(PackageMetadata.lastReport(name))
        val requirements = PackageMetadata.requirements(jar, JarInspector.inspect(jar), read, JarPackages.entryClassName(jar))
        assertTrue(requirements.none { it.ok == false }, requirements.joinToString { "${it.name}: ${it.detail}" })
        assertTrue(requirements.any { it.name == "greeting.txt" && it.ok == true })
        jar.delete(); PackageMetadata.configDir(name).deleteRecursively(); PackageMetadata.reportFile(name).delete()
    }

    @Test
    fun `a sidecar beside the jar overrides its tests, a failing test and a throwing call are reported`() {
        val name = "cfgside-${System.nanoTime()}.jar"
        val jar = buildJar(name, meta)
        PackageMetadata.writeSidecar(name, PackageMeta(tests = listOf(
            PackageTest(name = "wrong", input = "z", expect = "nothing like it"),
            PackageTest(name = "throws", input = "boom"),
        )))
        assertEquals(2, PackageMetadata.read(jar)!!.tests.size)
        assertEquals("1.2", PackageMetadata.read(jar)!!.version)            // the jar's own fields stay
        val report = JarPackages.test(name)
        assertFalse(report.ok)
        assertEquals(2, report.failed)
        assertTrue(report.results[0].reason!!.startsWith("expected"), report.results[0].reason)
        assertTrue(report.results[1].reason!!.contains("asked to fail"), report.results[1].reason)
        jar.delete(); PackageMetadata.sidecarFile(name).delete(); PackageMetadata.configDir(name).deleteRecursively(); PackageMetadata.reportFile(name).delete()
    }

    @Test
    fun `a jar without metadata gets a smoke run and a template to fill in`() {
        val name = "cfgnone-${System.nanoTime()}.jar"
        val jar = buildJar(name, null)
        assertEquals(null, PackageMetadata.read(jar))
        val report = JarPackages.test(name)
        assertTrue(report.smoke)
        assertTrue(report.ok, report.summary())
        assertEquals(listOf("none: hello", "property set", "no resource"), report.results.single().answer)
        val template = PackageMetadata.writeTemplate(name, "t.Cfg", 21)
        assertTrue(template.isFile)
        assertEquals("t.Cfg", PackageMetadata.read(jar)!!.entry)
        jar.delete(); template.delete(); PackageMetadata.configDir(name).deleteRecursively(); PackageMetadata.reportFile(name).delete()
    }
}
