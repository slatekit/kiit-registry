package kiit.registry

interface Module {
    /** The semantic name registry indexes this module under, e.g. "shared", "spaces" — not the `ModuleN_` loading-order prefix. */
    val name: String
    val registry: Registry

    /** This module's write surface into [registry] — every registration made through it is tagged with [name]. */
    val scope: RegistryScope get() = RegistryScope(registry, name)

    fun register()

    /** Lets [register] group related registrations under one block for readability; resolves against [scope]. */
    fun register(op: RegistryScope.() -> Unit) {
        op(this.scope)
    }
}