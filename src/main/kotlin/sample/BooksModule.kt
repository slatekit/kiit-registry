package sample

import kiit.registry.Module
import kiit.registry.Registry

class BooksModule(override val registry: Registry) : Module {

    override val name: String = "books"

    override fun register() {
        register {
            entity<Book>("sample", schema = "public", table = "book") { Book.create() }
            repo<InMemoryRepo<Book>>(Book::class) { InMemoryRepo<Book>() }

            // Reaches into AuthorsModule's repo by type + qualifier — modules register
            // independently, but resolution is global, not scoped to one module.
            service<Library>("sample", "library", "library") {
                Library(get<InMemoryRepo<Author>>(Author::class), get<InMemoryRepo<Book>>(Book::class))
            }
        }
    }
}
