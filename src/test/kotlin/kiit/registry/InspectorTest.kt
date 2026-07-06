package kiit.registry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InspectorTest {

    private class FakeApi
    private class FakeService
    private class FakeEntity
    private class FakeCache
    private class FakeQueue

    private fun put(resources: Resources, kind: ResourceKind, category: String, name: String, value: Any) {
        val id = ResourceId("", kind, category, value::class, "", name)
        resources.put(Resource(id, loader = { value }, isSingleton = true, loaded = false, instance = null))
    }

    @Test
    fun `getAllApis returns only Service api-category resources`() {
        val resources = Resources()
        put(resources, ResourceKind.Service, "api", "user-api", FakeApi())
        put(resources, ResourceKind.Service, "general", "user-service", FakeService())

        val apis = Inspector(resources).getAllApis()

        assertEquals(1, apis.size)
        assertTrue(apis[0] is FakeApi)
    }

    @Test
    fun `getAllEntities returns only Data entity-category resources`() {
        val resources = Resources()
        put(resources, ResourceKind.Data, "entity", "user", FakeEntity())
        put(resources, ResourceKind.Data, "repo", "user-repo", FakeService())

        val entities = Inspector(resources).getAllEntities()

        assertEquals(1, entities.size)
        assertTrue(entities[0] is FakeEntity)
    }

    @Test
    fun `getAllInfra returns every infra category, not just one`() {
        val resources = Resources()
        put(resources, ResourceKind.Infra, "cache", "cache-1", FakeCache())
        put(resources, ResourceKind.Infra, "queue", "queue-1", FakeQueue())
        put(resources, ResourceKind.Data, "entity", "user", FakeEntity())

        val infra = Inspector(resources).getAllInfra()

        assertEquals(2, infra.size)
    }

    @Test
    fun `getAllIds returns metadata without instantiating anything`() {
        val resources = Resources()
        var built = false
        val id = ResourceId("", ResourceKind.General, "widget", Any::class, "", "widget-1")
        resources.put(Resource(id, loader = { built = true; Any() }, isSingleton = true, loaded = false, instance = null))

        val ids = Inspector(resources).getAllIds()

        assertEquals(listOf(id), ids)
        assertEquals(false, built)
    }

    @Test
    fun `getMetrics reports only singletons that have actually been created`() {
        val resources = Resources()
        val resolvedId = ResourceId("", ResourceKind.General, "widget", FakeEntity::class, "", "resolved")
        val unresolvedId = ResourceId("", ResourceKind.General, "widget", FakeEntity::class, "", "unresolved")
        val nonSingletonId = ResourceId("", ResourceKind.General, "widget", FakeEntity::class, "", "fresh")

        resources.put(Resource(resolvedId, loader = { FakeEntity() }, isSingleton = true, loaded = false, instance = null, module = "shared"))
        resources.put(Resource(unresolvedId, loader = { FakeEntity() }, isSingleton = true, loaded = false, instance = null))
        resources.put(Resource(nonSingletonId, loader = { FakeEntity() }, isSingleton = false, loaded = false, instance = null))

        resources.get<FakeEntity>(resolvedId)
        resources.get<FakeEntity>(nonSingletonId)

        val metrics = Inspector(resources).getMetrics()

        assertEquals(1, metrics.size)
        assertEquals(resolvedId, metrics[0].id)
        assertEquals("shared", metrics[0].module)
        assertTrue(metrics[0].durationMillis >= 0)
    }
}
