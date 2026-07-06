package sample

/** A minimal in-memory repo, just for this sample — kiit-ace itself has no opinion on persistence. */
class InMemoryRepo<T> {
    private val items = mutableMapOf<Long, T>()
    private var nextId = 0L

    fun create(item: T): Long {
        nextId += 1
        items[nextId] = item
        return nextId
    }

    fun get(id: Long): T? = items[id]

    fun all(): List<T> = items.values.toList()
}
