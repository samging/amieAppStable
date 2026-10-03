import org.example.project.components.terminal.window.controller.ScriptLogger
val amie = bindings["logger"] as ScriptLogger
val constraints = mutableListOf<String>()
val goals = mutableListOf<String>()
for(i in 0..10){
	amie.log("instant")
}
