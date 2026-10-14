package org.example.project.components.terminal.window.controller.modules

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.project.components.terminal.integrations.amiePilot.InitMod
import org.example.project.components.terminal.integrations.amiePilot.InteractionModel
import org.example.project.components.terminal.window.controller.LogLevel
import org.example.project.components.terminal.window.controller.logInternal
import java.io.File
import java.time.Instant

/** A named group of models that work together: opened as one session, listened, validated and terminated as one */
@Serializable
data class ModelCollection(val name: String, val models: List<String> = emptyList(), val createdAt: String = Instant.now().toString())

@Serializable
private data class CollectionsFile(val collections: List<ModelCollection> = emptyList())

/** The collections, kept in AMP/modules/collections.json */
object ModelCollections {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val file: File get() = File(modulesDir, "collections.json")

    @Synchronized
    fun all(): List<ModelCollection> =
        runCatching { json.decodeFromString<CollectionsFile>(file.readText()).collections }.getOrDefault(emptyList()).sortedBy { it.name }

    fun find(name: String): ModelCollection? = all().firstOrNull { it.name == name }

    /** The collections a model belongs to */
    fun of(model: String): List<String> = all().filter { model in it.models }.map { it.name }

    /** Puts the model into the collection, creating the collection when it is new */
    @Synchronized
    fun add(collection: String, model: String) {
        val name = collection.trim().replace(Regex("""[^A-Za-z0-9._ -]"""), "_")
        require(name.isNotEmpty()) { "a collection needs a name" }
        val current = all()
        val existing = current.firstOrNull { it.name == name }
        val updated = existing?.copy(models = (existing.models + model).distinct()) ?: ModelCollection(name, listOf(model))
        write(current.filter { it.name != name } + updated)
        logInternal("collections: '$model' added to '$name'")
    }

    @Synchronized
    fun remove(collection: String, model: String) {
        val current = all()
        val existing = current.firstOrNull { it.name == collection } ?: return
        write(current.filter { it.name != collection } + existing.copy(models = existing.models - model))
        logInternal("collections: '$model' removed from '$collection'")
    }

    @Synchronized
    fun delete(collection: String) {
        write(all().filter { it.name != collection })
        logInternal("collections: '$collection' deleted")
    }

    private fun write(collections: List<ModelCollection>) {
        modulesDir.mkdirs()
        runCatching { file.writeText(json.encodeToString(CollectionsFile(collections.sortedBy { it.name }))) }
            .onFailure { logInternal("collections: couldn't write ${file.name}: ${it.message}", LogLevel.ERROR) }
    }

    /** Where the sessions of a collection log what they were asked and answered */
    fun sessionsLogFile(collection: String): File = File(modulesDir, "collections/$collection.sessions.log")
}

/**
 * A collection behind the inference / getStatus / close interface: the text goes to every member, and the
 * answers come back as one list sorted by score, each prefixed with the model that gave it (`dummy/lights:0.61`).
 * `initMod("name")` on a collection name opens one of these.
 */
class CollectionModel(val collection: ModelCollection) : InteractionModel {
    private val members: Map<String, InteractionModel> = collection.models.associateWith { InitMod(it).create() }

    override fun inference(text: String): List<String> =
        members.flatMap { (model, m) -> m.inference(text).map { answer -> Triple(model, answer, answer.substringAfterLast(':').toDoubleOrNull() ?: 0.0) } }
            .sortedByDescending { it.third }
            .map { (model, answer, _) -> "$model/$answer" }

    override fun getStatus(): Any = mapOf(
        "collection" to collection.name,
        "models" to collection.models,
        "members" to members.mapValues { it.value.getStatus() },
    )

    override fun close() {
        members.forEach { (model, m) -> runCatching { m.close() }.onFailure { logInternal("collection ${collection.name}: $model close failed: ${it.message}", LogLevel.ERROR) } }
    }
}
