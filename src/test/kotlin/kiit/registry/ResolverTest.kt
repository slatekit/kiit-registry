package kiit.registry

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ResolverTest {

    private class FakeWidget

    private fun register(resources: Resources, qualifier: String? = null): ResourceId {
        val id = ResourceId("acme", ResourceKind.General, "widget", FakeWidget::class, "", "widget-1", qualifier = qualifier)
        resources.put(Resource(id, loader = { FakeWidget() }, isSingleton = true, loaded = false, instance = null))
        return id
    }

    @Test
    fun `get resolves the single resource registered for a type`() {
        val resources = Resources()
        register(resources)
        val resolver = Resolver(resources)

        val widget: FakeWidget = resolver.get()

        assertNotNull(widget)
    }

    @Test
    fun `get throws a clear error when nothing is registered for a type`() {
        val resolver = Resolver(Resources())

        val error = assertFailsWith<IllegalStateException> { resolver.get<FakeWidget>() }

        assertTrue(error.message!!.contains("No resource registered"))
    }

    @Test
    fun `string and class qualifier overloads resolve the same registration`() {
        val resources = Resources()
        register(resources, qualifier = FakeWidget::class.qualifiedName)
        val resolver = Resolver(resources)

        val byString: FakeWidget = resolver.get(FakeWidget::class.qualifiedName!!)
        val byClass: FakeWidget = resolver.get(FakeWidget::class)

        assertSame(byString, byClass)
    }

    @Test
    fun `getById resolves by exact id even when the type is ambiguous`() {
        val resources = Resources()
        val id1 = register(resources, qualifier = "one")
        register(resources, qualifier = "two")
        val resolver = Resolver(resources)

        val widget: FakeWidget = resolver.getById(id1)

        assertNotNull(widget)
        assertFailsWith<IllegalStateException> { resolver.get<FakeWidget>() }
    }
}
