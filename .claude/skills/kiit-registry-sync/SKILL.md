---
name: kiit-registry-sync
description: Read the Kiit Registry (AppRegistry.kt + Module*.kt) and update the .ace/index/ files. Use when the Registry has changed and the Index needs to catch up.
---

# kiit-registry-sync

Keeps `.ace/index/` in sync with what's actually registered in code. The Index is a table of
contents — pointers to components, not their detail.

## What it does

- Reads every module's registration code (no compiling/running the app)
- Maps each registration to an architecture term (`ace.model`, `ace.api`, etc.)
- Writes/updates one Index entry per component
- Reports exactly what was added, updated, or removed

## Arguments

- `--module <name>` — optional, sync only one module (e.g. `--module shared`). Default: all modules.
- `--dry-run` — optional, show what would change without writing files.

## Reads

- `api/.ace/ace.config.json5` — for `registry` (entry-point file), `spec.provider`, `index_path`
- The registry entry point and every module file it lists (currently
  `api/src/main/kotlin/life/blend/api/setup/AppRegistry.kt` + `Module0_Startup.kt`/`Module1_Shared.kt`/`Module2_Spaces.kt`)
- `api/.ace/frameworks/kiit.provider.json5` — maps `(kind, category)` → architecture term
- Existing `.ace/index/*.index.json5` files, to preserve any manually-set fields (e.g. `confirmed`)

Related docs: [ace_pack/06-index.md](../../../ace_pack/06-index.md), [ace_pack/07-registry.md](../../../ace_pack/07-registry.md), [ace_pack/13-engine.md](../../../ace_pack/13-engine.md)

## Process

1. Parse the module list out of the registry entry point.
2. For each module file, find calls to the registration functions: `config`, `infra`, `service`,
   `entity`, `repo`, `api`, `folder`, `single`, `create`.
3. For an individual call (`config`/`infra`/`service`/`entity`/`repo`/`api`/`single`/`create`):
   - Take `kind` + `category` straight from the call (or from the function name, e.g. `entity()`
     always means `kind=Data, category="entity"`)
   - Map `(kind, category)` to an architecture term via the provider file
   - Take the module's name from the enclosing `Module.name`
   - `source` is left unresolved unless a `folder()` covers that same directory (see below)
4. For a `folder(kind, category, path)` call:
   - List the `.kt` files under `path`
   - Emit one Index entry per file, all sharing that `kind`/`category`/`module`, `source` = the file's path
5. Merge with the existing Index: keep entries whose registration didn't change, update ones that
   did, remove ones no longer registered (never silently — always report a removal).
6. Write the updated Index files.

## Output

- Updated `api/.ace/index/*.index.json5` (format: [ace_pack/06-index.md](../../../ace_pack/06-index.md))
- A run summary (see below)

## Example

```
/kiit-registry-sync

Reading registry: api/src/main/kotlin/life/blend/api/setup/AppRegistry.kt
Modules: startup, shared, spaces

+ Added:    UserApi        api        module=shared
+ Added:    User           model      module=shared   source=life/blend/api/domain/models/User.kt
~ Updated:  Device         model      module=shared   (table: user -> device)
- Removed:  OldEntity      not registered anymore

Sync summary: 2 added, 1 updated, 1 removed, 12 unchanged
```

## Summary format

Every run ends with one line in this exact shape:

```
Sync summary: <added> added, <updated> updated, <removed> removed, <unchanged> unchanged
```
