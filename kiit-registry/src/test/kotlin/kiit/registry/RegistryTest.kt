package kiit.registry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RegistryTest {

    private interface Entity<TId : Comparable<TId>> {
        fun identity(): TId
        fun isPersisted(): Boolean
    }

    private data class FakeWidget(val id: Long = 0, val name: String = "") : Entity<Long> {
        override fun identity(): Long = id
        override fun isPersisted(): Boolean = id > 0L
    }

    private data class FakeGadget(val id: Long = 0, val name: String = "") : Entity<Long> {
        override fun identity(): Long = id
        override fun isPersisted(): Boolean = id > 0L
    }

    private class FakeCounter

    /** Stands in for a real repo type (e.g. kiit-data's SimpleRepo) — just enough to prove two
     * instances sharing an erased type stay independent once disambiguated by qualifier. */
    private class FakeRepo<T> {
        private val items = mutableMapOf<Long, T>()
        private var nextId = 0L

        fun create(item: T): Long {
            nextId += 1
            items[nextId] = item
            return nextId
        }

        fun get(id: Long): T? = items[id]
    }

    @Test
    fun `single caches one instance across resolves`() {
        val registry = Registry()
        RegistryScope(registry).single<FakeCounter>("acme", ResourceKind.General, "counter", "c1") { FakeCounter() }

        val first = registry.resolver.get<FakeCounter>()
        val second = registry.resolver.get<FakeCounter>()

        assertSame(first, second)
    }

    @Test
    fun `create builds a fresh instance on every resolve`() {
        val registry = Registry()
        RegistryScope(registry).create<FakeCounter>("acme", ResourceKind.General, "counter", "c1") { FakeCounter() }

        val first = registry.resolver.get<FakeCounter>()
        val second = registry.resolver.get<FakeCounter>()

        assertNotSame(first, second)
    }

    @Test
    fun `registering the same id twice throws`() {
        val registry = Registry()

        assertFailsWith<IllegalStateException> {
            RegistryScope(registry).apply {
                infra<FakeCounter>("aws", "widget", "widget-1") { FakeCounter() }
                infra<FakeCounter>("aws", "widget", "widget-1") { FakeCounter() }
            }
        }
    }

    @Test
    fun `entity and repo for the same domain type do not collide`() {
        val registry = Registry()
        RegistryScope(registry).apply {
            entity<FakeWidget>("app", schema = "public", table = "widget") { FakeWidget() }
            repo<FakeRepo<FakeWidget>>(FakeWidget::class) { FakeRepo<FakeWidget>() }
        }

        val entity = registry.resolver.get<FakeWidget>()
        val repo = registry.resolver.get<FakeRepo<FakeWidget>>(FakeWidget::class)

        assertEquals(FakeWidget(), entity)
        assertSame(repo, registry.resolver.get<FakeRepo<FakeWidget>>(FakeWidget::class))
    }

    @Test
    fun `repo qualifier disambiguates resources sharing an erased type`() {
        val registry = Registry()
        RegistryScope(registry).apply {
            repo<FakeRepo<FakeWidget>>(FakeWidget::class) { FakeRepo<FakeWidget>() }
            repo<FakeRepo<FakeGadget>>(FakeGadget::class) { FakeRepo<FakeGadget>() }
        }

        val widgetRepo = registry.resolver.get<FakeRepo<FakeWidget>>(FakeWidget::class)
        val gadgetRepo = registry.resolver.get<FakeRepo<FakeGadget>>(FakeGadget::class)

        val widgetId = widgetRepo.create(FakeWidget(name = "w1"))

        assertEquals(FakeWidget(name = "w1"), widgetRepo.get(widgetId))
        assertNull(gadgetRepo.get(widgetId))
    }

    @Test
    fun `resolving an ambiguous type without a qualifier throws`() {
        val registry = Registry()
        RegistryScope(registry).apply {
            repo<FakeRepo<FakeWidget>>(FakeWidget::class) { FakeRepo<FakeWidget>() }
            repo<FakeRepo<FakeGadget>>(FakeGadget::class) { FakeRepo<FakeGadget>() }
        }

        assertFailsWith<IllegalStateException> { registry.resolver.get<FakeRepo<*>>() }
    }

    @Test
    fun `resources registered during a module's register() are tagged with that module's name`() {
        val registry = Registry()
        val fakeModule = object : Module {
            override val name: String = "fake"
            override val registry: Registry = registry
            override fun register() {
                register { infra<FakeCounter>("acme", "widget", "widget-1") { FakeCounter() } }
            }
        }

        registry.register(listOf(fakeModule))

        assertEquals(1, registry.inspector.findByModule("fake").size)
        assertEquals(0, registry.inspector.findByModule("other").size)
    }

    @Test
    fun `singleton creation records start, finish, and duration`() {
        val registry = Registry()
        RegistryScope(registry).single<FakeCounter>("acme", ResourceKind.General, "counter", "c1") { FakeCounter() }

        registry.resolver.get<FakeCounter>()

        val resource = registry.resources.resources().single { it.id.name == "c1" }
        assertNotNull(resource.startedAtMillis)
        assertNotNull(resource.finishedAtMillis)
        assertEquals(resource.finishedAtMillis!! - resource.startedAtMillis!!, resource.durationMillis)
    }

    @Test
    fun `non-singleton creation does not record timing`() {
        val registry = Registry()
        RegistryScope(registry).create<FakeCounter>("acme", ResourceKind.General, "counter", "c1") { FakeCounter() }

        registry.resolver.get<FakeCounter>()

        val resource = registry.resources.resources().single { it.id.name == "c1" }
        assertNull(resource.durationMillis)
    }

    @Test
    fun `override replaces the resolved value for a type without touching the real registration`() {
        val registry = Registry()
        RegistryScope(registry).single<FakeCounter>("acme", ResourceKind.General, "counter", "c1") { FakeCounter() }
        val mock = FakeCounter()

        registry.override(mock)

        assertSame(mock, registry.resolver.get<FakeCounter>())
    }

    @Test
    fun `override disambiguates by qualifier the same way get does`() {
        val registry = Registry()
        RegistryScope(registry).apply {
            repo<FakeRepo<FakeWidget>>(FakeWidget::class) { FakeRepo<FakeWidget>() }
            repo<FakeRepo<FakeGadget>>(FakeGadget::class) { FakeRepo<FakeGadget>() }
        }
        val mockWidgetRepo = FakeRepo<FakeWidget>()

        registry.override(FakeWidget::class, mockWidgetRepo)

        assertSame(mockWidgetRepo, registry.resolver.get<FakeRepo<FakeWidget>>(FakeWidget::class))
        assertTrue(mockWidgetRepo !== registry.resolver.get<FakeRepo<FakeGadget>>(FakeGadget::class))
    }

    @Test
    fun `override does not record creation timing`() {
        val registry = Registry()
        RegistryScope(registry).single<FakeCounter>("acme", ResourceKind.General, "counter", "c1") { FakeCounter() }

        registry.override(FakeCounter())
        registry.resolver.get<FakeCounter>()

        val resource = registry.resources.resources().single { it.id.name == "c1" }
        assertNull(resource.durationMillis)
    }
}
