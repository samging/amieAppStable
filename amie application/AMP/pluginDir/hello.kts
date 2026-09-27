// hello.kts
import org.example.project.components.terminal.window.controller.ScriptLogger

val logger = bindings["logger"] as ScriptLogger
logger.log("Hello from script! Fixed the class access issue.")
logger.log("your mom stinks!")
