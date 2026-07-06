package kiit.registry

/**
 * Builds the actual value for a registered [ResourceId].
 *
 * Every registration on [Registry] (`infra`, `entity`, `api`, ...) stores one of these — it
 * runs lazily, the first time the resource is resolved, not at registration time. See
 * [Resources.get] for the lazy-load-and-cache behavior.
 */
typealias Loader = (ResourceId) -> Any