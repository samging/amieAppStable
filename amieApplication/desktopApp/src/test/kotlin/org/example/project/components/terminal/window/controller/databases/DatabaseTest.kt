package org.example.project.components.terminal.window.controller.databases

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DatabaseTest {
    @Test
    fun `the postgres driver is on the classpath and a bad host is one DatabaseException`() {
        Class.forName("org.postgresql.Driver")
        val db = Database("probe", DatabaseConfig(url = "jdbc:postgresql://127.0.0.1:1/nothing?connectTimeout=1&loginTimeout=1", user = "x", password = ""))
        val e = assertFailsWith<DatabaseException> { db.query("select 1") }
        assertTrue(e.message!!.startsWith("database 'probe': can't connect to jdbc:postgresql://127.0.0.1:1/nothing"), e.message)
        assertEquals(0, db.runs)
        db.close()
    }

    @Test
    fun `ENV placeholders resolve from the environment`() {
        val config = DatabaseConfig(user = "\$ENV:AMIE_TEST_NO_SUCH_VAR", password = "plain")
        assertEquals("", config.resolved(config.user))
        assertEquals("plain", config.resolved(config.password))
        assertEquals(System.getenv("PATH") ?: "", config.resolved("\$ENV:PATH"))
    }
}
