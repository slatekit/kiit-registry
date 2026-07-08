package kiit.registry

import kotlin.reflect.KClass


/**
 * Read-only, simple-lookup half of the Registry/Resolver split — "Registry defines, Resolver
 * resolves." Used from inside dependency-construction lambdas to pull in other
 * already-registered resources, e.g. `UserApi(get<UserService>())` inside a [Registry.api]
 * registration.
 *
 * All lookups here resolve by **type**, optionally plus a qualifier — not by a
 * fully-specified [ResourceId] (see [getById] for that). For bulk introspection
 * ("give me every API"), see [Inspector] instead.
 */
class Resolver(val res: Resources) {

    /**
     * Throws if zero or more than one resource shares that type — when more than one does,
     * use [get] with a qualifier, or [getById], to disambiguate.
     *
     * Example:
     * ```kotlin
     * val service = get<UserService>()
     * ```
     *
     * @return the single resource registered with `kls == T::class`.
     * @throws IllegalStateException if no resource, or more than one, is registered for `T`.
     */
    inline fun <reified T> get(): T = res.get(idFor<T>())

    /**
     * A general-purpose named value, the same role Koin's `named(String)` plays. Needed when
     * multiple resources share the same type, e.g. two repos both erased to `SimpleRepo::class`.
     *
     * Example:
     * ```kotlin
     * val repo = get<SimpleRepo<User>>(User::class.qualifiedName!!)
     * ```
     *
     * @param qualifier the named value to match against [ResourceId.qualifier].
     * @return the resource registered with `kls == T::class` **and** the given [qualifier].
     * @throws IllegalStateException if no resource, or more than one, matches both `T` and [qualifier].
     */
    inline fun <reified T> get(qualifier: String): T = res.get(idFor<T>(qualifier))

    /**
     * Type-safe convenience over [get] — same role as Koin's `named<T>()`. The class is
     * flattened to its qualified name and matched against [ResourceId.qualifier].
     *
     * Example:
     * ```kotlin
     * val repo = get<SimpleRepo<User>>(User::class)
     * ```
     *
     * @param qualifier the class to flatten to a name and match against [ResourceId.qualifier].
     * @return the resource registered with `kls == T::class` and the given [qualifier].
     */
    inline fun <reified T> get(qualifier: KClass<*>): T {
        return get(qualifier.qualifiedName!!)
    }

    /**
     * One id always maps to exactly one resource, so this never throws for ambiguity, only if
     * nothing is registered under [id].
     *
     * Example:
     * ```kotlin
     * val queue: Queue<String> = getById(someKnownResourceId)
     * ```
     *
     * @param id the exact, fully-specified resource identity to resolve.
     * @return the resolved value, cast to [T].
     */
    fun <T> getById(id: ResourceId): T {
        return res.get(id)
    }

    /**
     * The lookup [get] is built on, extracted so [Registry.override] can find the same target
     * id without duplicating the disambiguation logic.
     *
     * @return the id of the single resource registered with `kls == T::class`.
     * @throws IllegalStateException if no resource, or more than one, is registered for `T`.
     */
    inline fun <reified T> idFor(): ResourceId {
        val kls = T::class
        val matches = res.idsByClass(kls)
        return when (matches.size) {
            0 -> throw IllegalStateException("No resource registered for type '${kls.qualifiedName}'")
            1 -> matches[0]
            else -> throw IllegalStateException(
                "Multiple resources registered for type '${kls.qualifiedName}': " +
                    "${matches.map { it.id }}. Use idFor(qualifier) or getById(id) to disambiguate."
            )
        }
    }

    /**
     * The lookup [get] (qualified overload) is built on, extracted so [Registry.override] can
     * find the same target id without duplicating the disambiguation logic.
     *
     * @param qualifier the named value to match against [ResourceId.qualifier].
     * @return the id of the resource registered with `kls == T::class` **and** [qualifier].
     * @throws IllegalStateException if no resource, or more than one, matches both `T` and [qualifier].
     */
    inline fun <reified T> idFor(qualifier: String): ResourceId {
        val kls = T::class
        val matches = res.idsByClassQualifier(kls, qualifier)
        return when (matches.size) {
            0 -> throw IllegalStateException("No resource registered for type '${kls.qualifiedName}' qualifier '$qualifier'")
            1 -> matches[0]
            else -> throw IllegalStateException(
                "Multiple resources registered for type '${kls.qualifiedName}' qualifier '$qualifier': ${matches.map { it.id }}"
            )
        }
    }
}
