package kiit.registry

import kotlin.reflect.KClass


/**
 * The broad architectural role a [ResourceId] plays.
 *
 * This is the top-level axis resources are grouped by — e.g. [Inspector.getAllInfra] returns
 * everything registered under [ResourceKind.Infra], regardless of vendor or subtype
 * ([ResourceId.category]).
 */
enum class ResourceKind(val value: Int) {
    Config(1),
    Infra(2),
    Vendor(3),
    Data(4),
    App (5),
    Service(6),
    General(7),
}


/**
 * The canonical, structured identity of a registered resource.
 *
 * Every registration on [Registry] builds one of these. It plays the same role an ARN plays
 * for AWS resources, or a group/version/kind + namespace/name tuple plays in Kubernetes — a
 * stable structured key, plus one derived string form ([id]) for display/logging.
 *
 * Example:
 * ```kotlin
 * val id = ResourceId("aws", ResourceKind.Infra, "queue", Queue::class, "", "queue-user1")
 * id.id // "aws:Infra:queue::queue-user1:"
 * ```
 *
 * @property company vendor/owner namespace, e.g. `"aws"`. Empty string when not applicable.
 * @property kind the broad role, see [ResourceKind].
 * @property category the subtype within [kind], e.g. `"cache"`/`"queue"`/`"store"` under
 *   [ResourceKind.Infra], or `"entity"`/`"repo"` under [ResourceKind.Data].
 * @property kls the Kotlin type of the value this id resolves to — what the registration's
 *   loader actually produces (see [Registry.bind]).
 * @property parent a grouping/namespace value; meaning depends on the registration function
 *   (e.g. schema for entities, area for apis).
 * @property name the resource's own name, e.g. `"queue-user1"`, `"user"`, `"UserApi"`.
 * @property version optional version tag, defaults to `""`. Included in identity — two
 *   versions of the same name/kls are distinct resources.
 * @property qualifier an optional general-purpose named value used to disambiguate resources
 *   that otherwise share the same [kls] — the same role Koin's `named(...)` plays. Used today
 *   by [Registry.repo] to tell `SimpleRepo<User>` apart from `SimpleRepo<Device>`, since both
 *   share `kls = SimpleRepo::class`.
 */
data class ResourceId(
    val company: String,
    val kind: ResourceKind,
    val category: String,
    val kls: KClass<*>,
    val parent:String,
    val name: String,
    val version: String = "",
    val qualifier: String? = null,
) {
    /** Single delimiter (`:`), fixed field order — safe to split for display, logging, or a future prefix-based lookup. */
    val id: String = "$company:${kind.name}:$category:$parent:$name:$version" +
        (qualifier?.let { ":$it" } ?: "")
}


/**
 * The stored registration record: a [ResourceId] plus everything needed to lazily produce
 * and, if singleton, cache its value.
 *
 * Created by [Registry.register] and stored via [Resources.put]. Not built directly by module
 * authors — use the typed functions on [Registry] instead (e.g. [Registry.infra]).
 *
 * @property id the resource's canonical identity.
 * @property loader builds the resource's value; invoked lazily, see [Loader].
 * @property isSingleton when true, [loader] runs once and [instance] is cached and reused on
 *   every later resolve; when false, [loader] runs fresh on every resolve (see [Registry.single]
 *   vs [Registry.create]).
 * @property loaded whether [instance] currently holds a cached value (only meaningful when [isSingleton]).
 * @property instance the cached value, once loaded; `null` until then.
 * @property module the [Module.name] that performed this registration —
 *   set automatically by [Registry.register] (the `List<Module>` overload), `""` if a resource
 *   was registered outside that flow. Deliberately not part of [ResourceId]: it's provenance
 *   about the registration, not part of what makes the resource unique — including it in
 *   identity would let two modules register the same id without tripping [Resources.put]'s
 *   collision check.
 * @property startedAtMillis wall-clock time ([System.currentTimeMillis]) the singleton's loader
 *   started running; `null` until the first resolve. Diagnostics only — never set for
 *   non-singleton resources, which are rebuilt on every resolve and have no single "creation" moment.
 * @property finishedAtMillis wall-clock time the singleton's loader finished; `null` until the first resolve.
 * @property durationMillis [finishedAtMillis] minus [startedAtMillis]; `null` until the first resolve.
 */
data class Resource(
    val id: ResourceId,
    val loader: Loader,
    val isSingleton: Boolean,
    val loaded: Boolean,
    val instance: Any?,
    val module: String = "",
    val startedAtMillis: Long? = null,
    val finishedAtMillis: Long? = null,
    val durationMillis: Long? = null,
)


