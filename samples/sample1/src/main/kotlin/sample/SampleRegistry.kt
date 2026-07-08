package sample

import kiit.registry.Registry

/** The sample's composition root — same shape a real app's AppRegistry would take. */
object SampleRegistry {
    val registry = Registry()

    init {
        registry.register(listOf(
            AuthorsModule(registry),
            BooksModule(registry),
        ))
    }
}
