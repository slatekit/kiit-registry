package sample


data class Author(
    val id: Long = 0,
    val name: String = "",
) : Entity<Long> {

    override fun identity(): Long = id
    override fun isPersisted(): Boolean = id > 0

    companion object {
        fun create(): Author = Author()
    }
}


interface Entity<TId: Comparable<TId>> {
    fun identity(): TId

    fun isPersisted(): Boolean
}