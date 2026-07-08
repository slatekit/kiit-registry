package kiit.registry


/**
 * A snapshot of one singleton's creation cost, projected from [Resource] so callers don't have
 * to deal with its internals ([Resource.loader], [Resource.instance]). See
 * [Resource.startedAtMillis]/[Resource.finishedAtMillis]/[Resource.durationMillis].
 *
 * @property id the resource's identity.
 * @property module the module that registered it, see [Resource.module].
 * @property startedAtMillis when its loader started running.
 * @property finishedAtMillis when its loader finished.
 * @property durationMillis [finishedAtMillis] minus [startedAtMillis].
 */
data class ResourceMetric(
    val id: ResourceId,
    val module: String,
    val startedAtMillis: Long,
    val finishedAtMillis: Long,
    val durationMillis: Long,
)


/**
 * Read-only, bulk-introspection half of the registry's read side — answers "what exists?"
 * rather than "give me this one thing" (that's [Resolver]'s job). Intended for downstream
 * framework integration that needs to enumerate components, e.g. wiring every registered API
 * into Ktor routing, or every entity into an ORM.
 *
 * Every method here **resolves and returns actual instances**, not just [ResourceId] metadata —
 * calling any of these instantiates (or reuses, if already cached) every match. The exception is
 * [getMetrics]/[showMetrics], which report on singletons already created rather than triggering
 * new creation.
 */
class Inspector(val res: Resources) {

    /** @return every registered id, across every kind/category — metadata only, no instantiation. */
    fun getAllIds(): List<ResourceId> = res.ids()

    /**
     * Example:
     * ```kotlin
     * registry.inspector.getAllApis().forEach { api -> ktorRouting.register(api) }
     * ```
     * @return every registered API (`ResourceKind.Service` + category `"api"`), resolved to instances.
     */
    fun getAllApis(): List<Any> = findByKindCategory(ResourceKind.Service, "api")

    /**
     * Example:
     * ```kotlin
     * registry.inspector.getAllEntities().forEach { entity -> orm.configure(entity) }
     * ```
     * @return every registered entity (`ResourceKind.Data` + category `"entity"`), resolved to instances.
     */
    fun getAllEntities(): List<Any> = findByKindCategory(ResourceKind.Data, "entity")

    /** @return every registered infra component (`ResourceKind.Infra`), across every vendor/category, resolved to instances. */
    fun getAllInfra(): List<Any> = findByKind(ResourceKind.Infra)

    /**
     * @param kind the [ResourceKind] to filter by.
     * @return every resolved instance registered under [kind], regardless of category.
     */
    fun findByKind(kind: ResourceKind): List<Any> =
        res.idsByKind(kind).map { res.getAny(it) }

    /**
     * @param kind the [ResourceKind] to filter by.
     * @param category the subtype within [kind] to filter by.
     * @return every resolved instance registered under [kind] + [category].
     */
    fun findByKindCategory(kind: ResourceKind, category: String): List<Any> =
        res.idsByKindCategory(kind, category).map { res.getAny(it) }

    /**
     * @param module the module name to filter by, e.g. `"shared"` — see [Resource.module].
     * @return every resolved instance registered by [module].
     */
    fun findByModule(module: String): List<Any> =
        res.idsByModule(module).map { res.getAny(it) }

    /** Every folder declaration made so far — bulk category pointers, not individual resources. Metadata only. */
    fun getAllFolders(): List<FolderId> = res.folders()

    /**
     * @param kind the [ResourceKind] to filter by.
     * @param category the subtype within [kind] to filter by.
     * @return every folder declared under [kind] + [category].
     */
    fun findFoldersByKindCategory(kind: ResourceKind, category: String): List<FolderId> =
        res.foldersByKindCategory(kind, category)

    /**
     * Does not instantiate anything — reports only on singletons that have already been
     * resolved at least once. Resources never resolved, or registered as non-singleton via
     * [Registry.create], are excluded since they have no single "creation" moment to time.
     * @return one [ResourceMetric] per created singleton, in no particular order.
     */
    fun getMetrics(): List<ResourceMetric> =
        res.resources()
            .filter { it.durationMillis != null }
            .map { ResourceMetric(it.id, it.module, it.startedAtMillis!!, it.finishedAtMillis!!, it.durationMillis!!) }

    /** Prints [getMetrics] to stdout, slowest creation first — a quick way to spot expensive singleton construction during startup. */
    fun showMetrics() {
        getMetrics().sortedByDescending { it.durationMillis }.forEach { m ->
            println("${m.durationMillis}ms  ${m.id.id}  module=${m.module}")
        }
    }
}
