package kiit.registry

/**
 * A bulk declaration that an entire directory contains resources of one kind/category — for
 * numerous, architecturally-simple things (domain models, enums) that don't warrant individual
 * Registry entries with a per-instance loader. Distinct from [ResourceId]/[Resource]: not a
 * single resolvable value, just a pointer to a category that a sync tool expands later by
 * scanning [path].
 *
 * @property kind the broad architectural role everything under [path] shares.
 * @property category the subtype within [kind], e.g. `"entities"`, `"enums"`.
 * @property path the directory these resources live in, relative to a source root, e.g.
 *   `"/life/blend/api/domain/models"`. Validated to exist when declared, see [RegistryScope.folder].
 * @property module the module that made this declaration, see [Resource.module]. Unlike
 *   [ResourceId.qualifier], there's no separate qualifier field here — declarations aren't
 *   deduplicated or collision-checked ([Resources.putFolder] just appends), so nothing needs
 *   disambiguating; [module] already identifies which module made the declaration, and [path]
 *   already distinguishes declarations from each other.
 */
data class FolderId(
    val kind: ResourceKind,
    val category: String,
    val path: String,
    val module: String = "",
)
