package org.example.project.components.terminal.window.controller.envBuilder

import kotlin.test.Test
import kotlin.test.assertEquals

/** The env chooser rewrites the script's environment line, or puts one at the top, and the scan reads it back */
class EnvPlanTest {
    @Test
    fun `a script without an environment line gets one after its leading comments`() {
        val code = "// notes\n// more\nlog(\"hi\")\n"
        val out = EnvPlan.withEnv(code, "lab")
        assertEquals("// notes\n// more\nenvironment(SettingsEnv(\"lab\"))\nlog(\"hi\")\n", out)
        assertEquals("lab", EnvPlan.scan(out).envName)
    }

    @Test
    fun `an existing environment line is rewritten and its profile kept`() {
        val code = "environment(SettingsEnv(\"old\", \"prod\"))\nlog(\"hi\")"
        val out = EnvPlan.withEnv(code, "new")
        assertEquals("environment(SettingsEnv(\"new\", \"prod\"))\nlog(\"hi\")", out)
        val plan = EnvPlan.scan(out)
        assertEquals("new", plan.envName)
        assertEquals("prod", plan.activeProfile)
        assertEquals("environment(SettingsEnv(\"default\"))\nlog(\"hi\")", EnvPlan.withEnv("amie.import(SettingsEnv(\"x\"))\nlog(\"hi\")", "default"))
    }

    @Test
    fun `a script that is only comments gets the line first`() {
        assertEquals("environment(SettingsEnv(\"lab\"))\n// just a note", EnvPlan.withEnv("// just a note", "lab"))
    }
}
