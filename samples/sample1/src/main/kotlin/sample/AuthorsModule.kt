package sample

import kiit.registry.Module
import kiit.registry.Registry

class AuthorsModule(override val registry: Registry) : Module {

    override val name: String = "authors"

    override fun register() {
        register {
            entity<Author>("sample", schema = "public", table = "author") { Author.create() }
            repo<InMemoryRepo<Author>>(Author::class) { InMemoryRepo<Author>() }
        }
    }
}
