package kiit.registry

import kotlin.reflect.KClass


/**
 * The storage layer: holds every registered [Resource], keyed by its [ResourceId].
 *
 * This is the one place identity collisions are caught ([put]) and the one place lazy
 * instantiation + singleton caching happens ([get]/[getAny]). [Registry] writes to it;
 * [Resolver] and [Inspector] both read from it, for two different use cases — typed
 * single-item lookup vs. bulk introspection.
 */
class Resources() {
    private val data = mutableMapOf<ResourceId, Resource>()
    private val folders = mutableListOf<FolderId>()

    /**
     * Registers a resource. Registration is write-once — see [override] for the test-mocking path.
     *
     * @param res the resource to register.
     * @throws IllegalStateException if [res]'s id is already registered.
     */
    fun put(res: Resource) {
        val id = res.id
        check(!data.containsKey(id)) { "Resource already registered for id '${id.id}' ($id)" }
        data[id] = res
    }

    /**
     * Replaces whatever is registered under [id], if anything — unlike [put], this never throws
     * on collision, since the entire point is replacing something that's already there. Stores
     * [value] as an already-loaded singleton, so it's returned as-is on every resolve; no loader
     * runs, no creation timing is recorded.
     *
     * Intended for test-time mocking: build a real [Registry] (e.g. by rerunning the app's usual
     * module setup), then override one or two specific resources before resolving anything from
     * it — see [Registry.override].
     *
     * @param id the resource to replace.
     * @param value the fixed value to return for [id] from now on.
     * @param module provenance for the override, defaults to `""` — see [Resource.module].
     */
    fun override(id: ResourceId, value: Any, module: String = "") {
        data[id] = Resource(id, loader = { value }, isSingleton = true, loaded = true, instance = value, module = module)
    }

    /**
     * Metadata only — does not instantiate anything.
     * @return every registered id, across every kind/category.
     */
    fun ids(): List<ResourceId> = data.keys.toList()

    /**
     * Metadata only — does not instantiate anything. The full [Resource] records, including
     * creation timing ([Resource.startedAtMillis]/[Resource.finishedAtMillis]/[Resource.durationMillis])
     * for whichever singletons have already been resolved at least once.
     * @return every registered resource, across every kind/category.
     */
    fun resources(): List<Resource> = data.values.toList()

    /**
     * Metadata only.
     * @param kind the [ResourceKind] to filter by, e.g. every infra component.
     * @return every id registered under [kind].
     */
    fun idsByKind(kind: ResourceKind): List<ResourceId> = data.keys.filter { it.kind == kind }

    /**
     * Metadata only.
     * @param kind the [ResourceKind] to filter by.
     * @param category the subtype within [kind] to filter by, e.g. `"entity"`.
     * @return every id registered under [kind] + [category].
     */
    fun idsByKindCategory(kind: ResourceKind, category: String): List<ResourceId> =
        data.keys.filter { it.kind == kind && it.category == category }

    /**
     * Metadata only — see [Resolver.get].
     * @param kls the class to filter by.
     * @return every id whose [ResourceId.kls] equals [kls].
     */
    fun idsByClass(kls: KClass<*>): List<ResourceId> = data.keys.filter { it.kls == kls }

    /**
     * What qualified lookups in [Resolver.get] are built on. Metadata only.
     * @param kls the class to filter by.
     * @param qualifier the [ResourceId.qualifier] to filter by.
     * @return every id whose [ResourceId.kls] equals [kls] **and** [ResourceId.qualifier] equals [qualifier].
     */
    fun idsByClassQualifier(kls: KClass<*>, qualifier: String?): List<ResourceId> =
        data.keys.filter { it.kls == kls && it.qualifier == qualifier }

    /**
     * Metadata only. Unlike the other `idsBy*` filters, this reads [Resource.module] rather
     * than a [ResourceId] field — module is registration provenance, not part of identity.
     * @param module the [Resource.module] to filter by, e.g. `"shared"`.
     * @return every id registered by [module].
     */
    fun idsByModule(module: String): List<ResourceId> =
        data.values.filter { it.module == module }.map { it.id }

    /**
     * The first resolve of a singleton resource runs its loader and caches the result; every
     * later call returns that cached instance. Non-singleton resources (registered via
     * [Registry.create]) run the loader fresh every time.
     *
     * @param id the resource to resolve.
     * @return the resource's value, cast to [T].
     * @throws IllegalArgumentException if nothing is registered under [id].
     */
    fun <T> get(id: ResourceId): T {
        return resolve(id) as T
    }

    /**
     * Same as [get], without the generic cast — used by [Inspector], which resolves
     * heterogeneous types at once.
     * @param id the resource to resolve.
     * @return the resource's value.
     */
    fun getAny(id: ResourceId): Any {
        return resolve(id)
    }

    /**
     * Stores a folder declaration. No collision check like [put] — multiple modules may
     * legitimately declare adjacent/overlapping directories.
     * @param folder the declaration to store.
     */
    fun putFolder(folder: FolderId) { folders.add(folder) }

    /** @return every folder declaration made so far. */
    fun folders(): List<FolderId> = folders.toList()

    /**
     * @param kind the [ResourceKind] to filter by.
     * @param category the subtype within [kind] to filter by.
     * @return every folder declared under [kind] + [category].
     */
    fun foldersByKindCategory(kind: ResourceKind, category: String): List<FolderId> =
        folders.filter { it.kind == kind && it.category == category }

    private fun resolve(id: ResourceId): Any {
        val item = data[id]
        require(item != null) { "No resource registered for id '${id.id}' ($id)" }
        if (!item.isSingleton) {
            return item.loader(item.id)
        }
        return when(item.loaded) {
            true -> {
                item.instance!!
            }
            false -> {
                val startedAt = System.currentTimeMillis()
                val instance = item.loader(item.id)
                val finishedAt = System.currentTimeMillis()
                val withInstance = item.copy(
                    instance = instance,
                    loaded = true,
                    startedAtMillis = startedAt,
                    finishedAtMillis = finishedAt,
                    durationMillis = finishedAt - startedAt,
                )
                data[id] = withInstance
                instance
            }
        }
    }
}