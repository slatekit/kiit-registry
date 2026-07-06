package kiit.registry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ResourceIdTest {

    private class FakeWidget

    @Test
    fun `id string uses a single delimiter in company-kind-category-parent-name-version order`() {
        val id = ResourceId("aws", ResourceKind.Infra, "queue", FakeWidget::class, "public", "queue-user1", "1")

        assertEquals("aws:Infra:queue:public:queue-user1:1", id.id)
    }

    @Test
    fun `id string appends the qualifier only when present`() {
        val withoutQualifier = ResourceId("", ResourceKind.Data, "repo", FakeWidget::class, "", "Widget")
        val withQualifier = withoutQualifier.copy(qualifier = "life.blend.api.domain.models.Widget")

        assertEquals(":Data:repo::Widget:", withoutQualifier.id)
        assertEquals(":Data:repo::Widget::life.blend.api.domain.models.Widget", withQualifier.id)
    }

    @Test
    fun `two ids differing only by qualifier are not equal`() {
        val a = ResourceId("", ResourceKind.Data, "repo", FakeWidget::class, "", "Widget", qualifier = "A")
        val b = a.copy(qualifier = "B")

        assertNotEquals(a, b)
        assertNotEquals(a.id, b.id)
    }

    @Test
    fun `two ids with identical fields are equal`() {
        val a = ResourceId("aws", ResourceKind.Infra, "cache", FakeWidget::class, "", "cache-1")
        val b = ResourceId("aws", ResourceKind.Infra, "cache", FakeWidget::class, "", "cache-1")

        assertEquals(a, b)
    }
}
