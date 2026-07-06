package kiit.registry

import kotlin.reflect.KClass


/**
 * The engine underneath every [RegistryScope] — owns the actual state ([resources]/[resolver]/
 * [inspector]) and the shared low-level primitives ([bind], [register]) that every scope's typed
 * registration functions delegate into. Doesn't expose registration DSL functions directly —
 * those live on [RegistryScope], since each one needs a module label attached; get one via
 * [Module.scope], or construct one by hand for standalone/service-locator-style usage.
 *
 * Reading back what was registered happens through [resolver] (simple, typed lookups) or
 * [inspector] (bulk introspection) — see their docs. Both operate across every module, unlike
 * [RegistryScope]'s deliberately narrow per-module write surface.
 *
 * Example:
 * ```kotlin
 * val registry = Registry()
 * registry.register(listOf(Module0_Startup(registry), Module1_Shared(registry)))
 * val userApi = registry.resolver.get<UserApi>()
 * ```
 */
class Registry(val resources: Resources = Resources()) {
    val resolver = Resolver(resources)
    val inspector = Inspector(resources)

    /**
     * The central startup entry point that replaces manually calling `m.register()` on each
     * module one by one.
     *
     * Example:
     * ```kotlin
     * val registry = Registry()
     * registry.register(listOf(Module0_Startup(registry), Module1_Shared(registry)))
     * ```
     *
     * @param modules every module to register, in order.
     */
    fun register(modules: List<Module>) {
        modules.forEach { m -> m.register() }
    }

    /**
     * Shared by every typed registration function on [RegistryScope] (single/create/config/infra/
     * entity/repo/api/service): builds the [ResourceId], wraps the loader, and registers it. Kept
     * non-reified — takes `kls` explicitly — because some callers (e.g. [RegistryScope.repo])
     * record identity under a different class than the value they actually produce (entity class
     * vs SimpleRepo<T>).
     *
     * @param company vendor/owner namespace.
     * @param kind the broad architectural role.
     * @param category the subtype within [kind].
     * @param name the resource's own name.
     * @param kls the type of the value [op] produces.
     * @param parent grouping/namespace value.
     * @param version optional version tag.
     * @param singleton whether the resource is cached after first resolve, or rebuilt every time.
     * @param qualifier optional named value to disambiguate resources sharing [kls].
     * @param module the module registering this resource, see [Resource.module].
     * @param op builds the resource's value; receives its [ResourceId].
     */
    fun <T> bind(company:String, kind: ResourceKind, category:String, name:String, kls: KClass<*>, parent:String = "", version:String = "", singleton: Boolean, qualifier: String? = null, module: String = "", op:(ResourceId) -> T) {
        val id = ResourceId(company, kind, category, kls, parent, name, version, qualifier)
        val loader: (ResourceId) -> Any = { op(id) as Any }
        register(id, loader, singleton, module)
    }

    /**
     * The lowest-level registration primitive — stores a pre-built [id]/[loader] pair.
     * [bind] is the usual entry point; call this directly only when you already have a
     * [ResourceId] in hand.
     *
     * @param id the resource's identity.
     * @param loader builds the resource's value; runs lazily.
     * @param singleton whether the built value is cached and reused, or rebuilt on every resolve.
     * @param module the module registering this resource, see [Resource.module].
     */
    fun register(id:ResourceId, loader: Loader, singleton:Boolean = true, module: String = "") {
        val res = Resource(id, loader, isSingleton = singleton, false, instance = null, module = module)
        resources.put(res)
    }

    /**
     * Replaces the resource already registered for type [T] with [value] — for test-time
     * mocking: build a real Registry (e.g. by rerunning the app's usual module setup, see
     * `AppRegistry.fresh()`), then override one or two specific resources before resolving
     * anything from it. Never throws on collision — that's the point; it only fails if the
     * lookup itself is ambiguous, same as [Resolver.get].
     *
     * Example:
     * ```kotlin
     * val testRegistry = AppRegistry.fresh()
     * testRegistry.override<SimpleRepo<User>>(User::class, fakeUserRepo)
     * val api = testRegistry.resolver.get<UserApi>()
     * ```
     *
     * @param value the fixed value to return in place of the real registration from now on.
     * @throws IllegalStateException if zero or more than one resource is registered for `T`.
     */
    inline fun <reified T> override(value: T) {
        resources.override(resolver.idFor<T>(), value as Any)
    }

    /** Same as [override], disambiguated by a string qualifier — see [Resolver.get]. */
    inline fun <reified T> override(qualifier: String, value: T) {
        resources.override(resolver.idFor<T>(qualifier), value as Any)
    }

    /** Same as [override], disambiguated by a class qualifier — see [Resolver.get]. */
    inline fun <reified T> override(qualifier: KClass<*>, value: T) {
        override(qualifier.qualifiedName!!, value)
    }
}
