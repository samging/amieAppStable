import org.example.project.components.terminal.window.controller.ScriptLogger
val amie = bindings["logger"] as ScriptLogger
val constraints = mutableListOf<String>()
val goals = mutableListOf<String>()
amie.modelBuilder("pj",{})
