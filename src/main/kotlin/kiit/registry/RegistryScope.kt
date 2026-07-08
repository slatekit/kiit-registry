package kiit.registry

import kiit.entities.Entity
import java.io.File
import kotlin.reflect.KClass


/**
 * The per-module (or standalone) write surface into a [Registry] — every registration made
 * through a scope is tagged with [module], see [Resource.module]. Modules get one automatically
 * via [Module.scope]; for standalone/service-locator-style usage outside the module system,
 * construct one directly (`RegistryScope(registry)`, [module] defaults to `""`).
 *
 * Example:
 * ```kotlin
 * class Module1_Shared(override val registry: Registry) : Module {
 *     override fun register() {
 *         register {
 *             infra<SimpleCache>("aws", "cache", "cache-users") { res -> AwsCache(res) }
 *             entity<User>("app", schema = "public", table = "user") { User.create() }
 *             api<UserApi>("accounts", "user", "1") { UserApi(get<UserService>()) }
 *         }
 *     }
 * }
 * ```
 */
class RegistryScope(val registry: Registry, val module: String = "") {

    /**
     * The loader runs once, on first resolve, and the instance is cached and reused after
     * that. Prefer the specific functions ([config], [infra], [entity], [api], [service]) when
     * one fits; use this directly only for a kind/category combination none of them cover.
     *
     * Example:
     * ```kotlin
     * single<MyThing>("acme", ResourceKind.General, "widget", "widget-1") { MyThing() }
     * ```
     *
     * @param company vendor/owner namespace.
     * @param kind the broad architectural role.
     * @param category the subtype within [kind].
     * @param name the resource's own name.
     * @param parent grouping/namespace value; meaning depends on the caller.
     * @param version optional version tag.
     * @param op builds the resource's value; runs lazily on first resolve.
     */
    inline fun <reified T> single(company:String, kind: ResourceKind, category:String, name:String, parent:String = "", version:String = "", noinline op:(ResourceId) -> T) {
        registry.bind(company, kind, category, name, T::class, parent, version, singleton = true, module = module, op = op)
    }

    /**
     * The opposite of [single]: every resolve re-runs [op] and returns a fresh instance,
     * nothing is cached.
     *
     * Example:
     * ```kotlin
     * create<RequestId>("acme", ResourceKind.General, "req-id", "request-id") { RequestId.new() }
     * ```
     *
     * @param company vendor/owner namespace.
     * @param kind the broad architectural role.
     * @param category the subtype within [kind].
     * @param name the resource's own name.
     * @param parent grouping/namespace value; meaning depends on the caller.
     * @param version optional version tag.
     * @param op builds the resource's value; runs fresh on every resolve.
     */
    inline fun <reified T> create(company:String, kind: ResourceKind, category:String, name:String, parent:String = "", version:String = "", noinline op:(ResourceId) -> T) {
        registry.bind(company, kind, category, name, T::class, parent, version, singleton = false, module = module, op = op)
    }

    /**
     * Registers application-level configuration under [ResourceKind.Config] — startup args,
     * environments, and about/build-info. Exists specifically so those don't have to go through
     * [single] as a generic, kind-less escape hatch.
     *
     * Example:
     * ```kotlin
     * config<Envs>("app", "envs", "envs") {
     *     Envs(listOf(Env("dev", EnvMode.Dev, desc = "Dev environment")))
     * }
     * ```
     *
     * @param company vendor/owner namespace.
     * @param category the subtype within [ResourceKind.Config], e.g. `"args"`, `"envs"`, `"about"`.
     * @param name the resource's own name.
     * @param parent grouping/namespace value; meaning depends on the caller.
     * @param version optional version tag.
     * @param op builds the config value; runs lazily on first resolve.
     */
    inline fun <reified T> config(company:String, category:String, name:String, parent:String = "", version:String = "", noinline op:(ResourceId) -> T) {
        registry.bind(company, ResourceKind.Config, category, name, T::class, parent, version, singleton = true, module = module, op = op)
    }

    /**
     * Registers an infrastructure component (queue, cache, storage, ...) under [ResourceKind.Infra].
     *
     * Example:
     * ```kotlin
     * infra<SimpleQueue>("aws", "queue", "queue-user1") { res -> AwsQueue(res) }
     * ```
     *
     * @param company vendor namespace, e.g. `"aws"`.
     * @param category the infra subtype, e.g. `"cache"`, `"queue"`, `"store"`.
     * @param name the resource's own name, e.g. `"queue-user1"`.
     * @param parent grouping/namespace value; usually unused for infra.
     * @param version optional version tag.
     * @param op builds the infra component; receives its [ResourceId].
     */
    inline fun <reified T> infra(company:String, category:String, name:String, parent:String = "", version:String = "", noinline op:(ResourceId) -> T) {
        registry.bind(company, ResourceKind.Infra, category, name, T::class, parent, version, singleton = true, module = module, op = op)
    }

    /**
     * Registers a plain application service under [ResourceKind.Service] — for services that
     * aren't themselves an `@Api` class (see [api] for those), e.g. a domain service an API
     * class depends on.
     *
     * Example:
     * ```kotlin
     * service<UserService>("", "general", "accounts", "user", "1") { UserService(get<SimpleRepo<User>>(User::class)) }
     * ```
     *
     * @param company vendor/owner namespace.
     * @param category the subtype within [ResourceKind.Service].
     * @param name the resource's own name.
     * @param parent grouping/namespace value, e.g. the API area this service supports.
     * @param version optional version tag.
     * @param op builds the service; receives its [ResourceId].
     */
    inline fun <reified T> service(company:String, category:String, name:String, parent:String = "", version:String = "", noinline op:(ResourceId) -> T) {
        registry.bind(company, ResourceKind.Service, category, name, T::class, parent, version, singleton = true, module = module, op = op)
    }

    /**
     * Registers a domain entity under `ResourceKind.Data` / category `"entity"`. [schema] and
     * [table] are the class-level persistence config that, per the Registry design principle,
     * live here rather than on the entity's annotation (which stays purely structural).
     *
     * Example:
     * ```kotlin
     * entity<User>("app", schema = "public", table = "user") { User.create() }
     * ```
     *
     * @param company vendor/owner namespace.
     * @param schema the database schema this entity belongs to.
     * @param table the table name.
     * @param op builds a default/blank instance of the entity; receives its [ResourceId].
     */
    inline fun <reified T: Entity<Long>> entity(company:String, schema:String, table:String, noinline op: (ResourceId) -> T) {
        registry.bind(company, ResourceKind.Data, "entity", table, T::class, schema, "", singleton = true, module = module, op = op)
    }

    /**
     * Registers a repository under `ResourceKind.Data` / category `"repo"`. [T] is the actual
     * registered value type (e.g. `SimpleRepo<User>`), matching every other registration
     * function's convention that the reified type is what the loader produces.
     *
     * Example:
     * ```kotlin
     * repo<SimpleRepo<User>>(User::class) { SimpleRepo<User>() }
     * ```
     *
     * @param qualifier the entity this repo is for — needed because multiple repos otherwise
     *   share the same erased `kls` (`SimpleRepo::class`); see [Resolver.get] (qualified
     *   overload) for the matching lookup side.
     * @param op builds the repo; receives its [ResourceId].
     */
    inline fun <reified T> repo(qualifier: KClass<*>, noinline op: (ResourceId) -> T) {
        registry.bind("", ResourceKind.Data, "repo", T::class.simpleName!!, T::class, "", "", singleton = true, qualifier = qualifier.qualifiedName, module = module, op = op)
    }

    /**
     * Registers an `@Api` class under `ResourceKind.Service` / category `"api"`.
     *
     * Example:
     * ```kotlin
     * api<UserApi>("accounts", "user", "1") { UserApi(get<UserService>()) }
     * ```
     *
     * @param parent the API's area, e.g. `"accounts"`.
     * @param name the API's own name, e.g. `"user"`.
     * @param version the API version.
     * @param op builds the `@Api` instance; receives its [ResourceId].
     */
    inline fun <reified T> api(parent:String, name:String, version: String, noinline op:(ResourceId) -> T) {
        registry.bind("", ResourceKind.App, "api", name, T::class, parent, version, singleton = true, module = module, op = op)
    }

    /**
     * Declares that every resource under [path] is a [kind]/[category] — a bulk alternative to
     * registering each model/enum individually. Validates immediately that [path] (resolved
     * against [srcRoot]) is an existing directory; a sync tool expands the declaration later by
     * scanning it. The declaration is tagged with this scope's module automatically — no
     * separate qualifier needed, since [FolderId] has none to set.
     *
     * Example:
     * ```kotlin
     * folder(ResourceKind.Data, "entities", "/life/blend/api/domain/models")
     * folder(ResourceKind.Data, "enums",    "/life/blend/api/domain/enums")
     * ```
     *
     * @param kind the broad architectural role everything under [path] shares.
     * @param category the subtype within [kind], e.g. `"entities"`, `"enums"`.
     * @param path the directory these resources live in, relative to [srcRoot].
     * @param srcRoot the source root [path] is resolved against.
     * @throws IllegalStateException if the resolved directory doesn't exist.
     */
    fun folder(kind: ResourceKind, category: String, path: String, srcRoot: String = "src/main/kotlin") {
        val dir = File(srcRoot, path.removePrefix("/"))
        check(dir.isDirectory) { "folder() declared '$path' but no directory found at '${dir.path}'" }
        registry.resources.putFolder(FolderId(kind, category, path, module))
    }

    /**
     * Delegates to [Resolver.get] — resolves by type alone.
     * @return the single resource registered for `T`.
     */
    inline fun <reified T> get(): T = registry.resolver.get()

    /**
     * Delegates to [Resolver.get] with a string qualifier.
     * @param qualifier the named value to match against [ResourceId.qualifier].
     * @return the resource registered for `T` and [qualifier].
     */
    inline fun <reified T> get(qualifier: String): T = registry.resolver.get(qualifier)

    /**
     * Delegates to [Resolver.get] with a class qualifier.
     * @param qualifier the class to flatten to a name and match against [ResourceId.qualifier].
     * @return the resource registered for `T` and [qualifier].
     */
    inline fun <reified T> get(qualifier: KClass<*>): T = registry.resolver.get(qualifier)
}
