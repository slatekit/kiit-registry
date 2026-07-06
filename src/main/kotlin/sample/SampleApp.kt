package sample

fun main() {
    val registry = SampleRegistry.registry

    val authorRepo = registry.resolver.get<InMemoryRepo<Author>>(Author::class)
    val bookRepo = registry.resolver.get<InMemoryRepo<Book>>(Book::class)
    val library = registry.resolver.get<Library>()

    val authorId = authorRepo.create(Author(name = "Ursula K. Le Guin"))
    val bookId = bookRepo.create(Book(title = "The Left Hand of Darkness", authorId = authorId))

    println(library.describe(bookId))

    println()
    println("Registered resources:")
    registry.inspector.getAllIds().forEach { println("  ${it.id}") }
}
