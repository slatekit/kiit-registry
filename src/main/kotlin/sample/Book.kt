package sample


data class Book(
    val id: Long = 0,
    val title: String = "",
    val authorId: Long = 0,
) : Entity<Long> {

    override fun identity(): Long = id
    override fun isPersisted(): Boolean = id > 0

    companion object {
        fun create(): Book = Book()
    }
}
