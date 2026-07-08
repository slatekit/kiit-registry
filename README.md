<div align="center">

# kiit.registry

**A service locator that knows what it registered.**
(Work in progress)

[![Maven Central](https://img.shields.io/maven-central/v/dev.kiit/kiit-registry?color=blue)](https://central.sonatype.com/artifact/dev.kiit/kiit-registry)
[![Build](https://img.shields.io/github/actions/workflow/status/slatekit/kiit-registry/build.yml?branch=main)](https://github.com/slatekit/kiit-registry/actions)
[![License](https://img.shields.io/github/license/slatekit/kiit-registry)](./LICENSE)
[![Kotlin](https://img.shields.io/badge/kotlin-JVM-purple.svg)](https://kotlinlang.org)

Part of the [Kiit](https://www.kiit.dev) framework · [kiit.dev/registry](https://www.kiit.dev/registry) · [Blog post](https://www.kiit.dev/blog/kiit-registry) · [Video walkthrough](#)

</div>

---

## 📚 Table of Contents

- [ℹ️ About](#ℹ️-about)
- [🧩 The problem](#-the-problem)
- [💡 The idea](#-the-idea)
- [🚀 Quick start](#-quick-start)
- [🧠 Core concepts](#-core-concepts)
- [🤖 Why this matters for AI](#-why-this-matters-for-ai-assisted-development)
- [🛠️ Use cases](#️-use-cases)
- [✅ When to use this](#-when-to-use-this-and-when-not-to)
- [📦 Requirements](#-requirements)
- [🗺️ Roadmap](#️-roadmap)
- [🤝 Contributing](#-contributing)
- [📄 License](#-license)

---

## ℹ️ About

**kiit.registry** is a lightweight dependency/service registry for Kotlin. It does the job a typical DI container or service locator does — register a builder for a type, resolve an instance later — but it's built around one different idea: **every registration carries explicit metadata about its role in your architecture**, not just its type.

Concretely, that means a registration records things like: what *kind* of resource this is (infra, entity, repo, API, service), which module registered it, and how it's identified when there's more than one of a given type. That metadata isn't a side note — it's queryable, which is what makes the registry itself a live, accurate map of your app's architecture, usable by tooling, documentation generators, and AI assistants, not just your own application code at runtime.

It's a small, dependency-free library — you can adopt it on its own, independent of the rest of [Kiit](https://www.kiit.dev).

<div align="center">

*[diagram placeholder — before/after: opaque service locator vs. semantically-typed registry]*

**Figure 1.** A typical registry resolves types but discards meaning. kiit.registry keeps the meaning.

</div>

## 🧩 The problem

Most DI containers and service locators are good at one thing: giving you back an instance when you ask for a type. What they don't do is remember *what that thing is for*.

Ask a typical container "what infrastructure does this app touch?" or "what's registered under the `accounts` module?" or "list every AWS resource this service depends on" — and the honest answer is: it can't tell you. That information lived in someone's head, or in a wiki page that's a year out of date, or nowhere at all.

That's an onboarding problem for humans. It's a much bigger problem for AI tooling trying to reason about your codebase — an assistant can read your registration code, but it can't distinguish "this is a repository" from "this is an HTTP client" from "this is a queue consumer" unless that distinction is actually encoded somewhere.

## 💡 The idea

**kiit.registry is a service locator where every registration carries an explicit type, category, and identity — so the registry itself becomes a queryable map of your architecture, not just a lookup table.**

Nothing about *how* you use it should feel unfamiliar if you've used a DI container before. What's different is that registration is semantic by default, not just typed.

## 🚀 Quick start

**Gradle (Kotlin DSL):**

```kotlin
dependencies {
    implementation("dev.kiit:kiit-registry:0.1.0")
}
```

**Register something:**

```kotlin
class UserModule(override val registry: Registry) : Module {

    override val name: String = "users"

    override fun register() {
        register {
            entity<User>("app", schema = "public", table = "user") { User.create() }
            repo<SimpleRepo<User>>(User::class) { SimpleRepo<User>() }
            service<UserService>("", "general", "accounts", "user", "1") {
                UserService(get<SimpleRepo<User>>(User::class))
            }
        }
    }
}
```

**Resolve it:**

```kotlin
val userService = registry.resolve<UserService>()
```

**Inspect it:**

```kotlin
val allInfra = registry.inspect(ResourceKind.Infra)
val fromUsersModule = registry.inspect(module = "users")
```

That's the whole loop: register with meaning attached, resolve like normal, inspect when you need to know what's actually there.

## 🧠 Core concepts

| Term | What it is |
|---|---|
| **ResourceId** | A resource's identity — company, kind/category (e.g. `aws` / `queue`), type, name, and an optional qualifier to disambiguate resources that share a type. |
| **Resource** | A `ResourceId` plus a lazy builder function, plus metadata (singleton or not, which module registered it, how long it took to build). |
| **Resources** | Storage and lookup for all resources, plus bulk "this folder is a set of X" declarations. |
| **Registry** | Owns storage and the low-level write/override primitives. |
| **RegistryScope** | The write surface a module actually uses to register things — infra, entities, APIs, repos, config, and more. |
| **Resolver** | Read-only lookup by type, with an optional qualifier for disambiguation. |
| **Inspector** | Read-only bulk lookup — everything of a kind, everything from a module — plus simple diagnostics. |
| **Module** | A logical group of resources that registers through its own scope. |

## 🤖 Why this matters for AI

Because every registration is explicitly typed and categorized, an AI assistant working in a codebase that uses kiit.registry can answer questions a typical container leaves opaque:

1. "What infrastructure does this service depend on?"
2. "Which module owns the `UserService`?"
3. "What's registered as an API vs. an internal service?"

This is the same information a human would otherwise have to reconstruct by reading constructor chains across files. With kiit.registry, it's queryable directly — which is also what powers Kiit's architecture-aware tooling (generating README/architecture docs from what's actually registered, not what's documented separately and likely stale).

## 🛠️ Use cases

1. **Application wiring** — resolve dependencies by type instead of wiring constructors by hand.
2. **Tooling** — read everything registered, by kind, category, or module, for external tools (or AI assistants) to inspect.
3. **Diagnostics** — track how long each resource took to build.
4. **Testing** — override one or two registrations with mocks without touching the rest of your setup.
5. **Bulk registration** — declare "this folder is a set of X" instead of registering many similar things one at a time.

## ✅ When to use this

**Good fit if:**
1. You want dependency wiring *and* an accurate, queryable picture of your architecture as a side effect.
2. You're building tooling, documentation generation, or AI-assisted workflows that need to reason about what your app actually contains.
3. You'd rather register explicitly than rely on classpath scanning or annotation magic.

**Probably not necessary if:**
1. Your app is small enough that manual wiring is genuinely simpler.
2. You need Android-specific lifecycle-aware DI (Koin/Hilt are more purpose-built for that today).
3. You want zero ceremony at the registration site — explicit typing means slightly more to write per registration, in exchange for the registry knowing what it holds.

We'd rather be upfront about that tradeoff than pretend there isn't one.

## 📦 Requirements

1. Kotlin, JVM
2. No external runtime dependencies

*(Multiplatform support is on the roadmap — not yet available.)*

## 🗺️ Roadmap

- [ ] Kotlin Multiplatform support
- [ ] Additional artifact-generation skills (README/architecture.md generation from registry contents)
- [ ] Expanded inspector/diagnostics API

Track progress or open a discussion in [Issues](https://github.com/slatekit/kiit-registry/issues).

## 🤝 Contributing

Contributions are welcome — see [CONTRIBUTING.md](./CONTRIBUTING.md) for setup, build, and PR guidelines.

## 📄 License

[Apache License 2.0](./LICENSE)

---

<div align="center">

kiit.registry is one module of **[Kiit](https://www.kiit.dev)** — a lightweight, modular, 100% Kotlin framework for building server apps, APIs, CLIs, and jobs. Adopt one module at a time.

</div>
