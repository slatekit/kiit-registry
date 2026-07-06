package sample

/** Depends on both modules' repos — registered in [BooksModule], demonstrating that a module's
 * dependencies can reach across module boundaries via [kiit.registry.RegistryScope.get]. */
class Library(private val authors: InMemoryRepo<Author>, private val books: InMemoryRepo<Book>) {

    fun describe(bookId: Long): String {
        val book = books.get(bookId) ?: return "Unknown book"
        val author = authors.get(book.authorId)
        return "\"${book.title}\" by ${author?.name ?: "Unknown author"}"
    }
}
