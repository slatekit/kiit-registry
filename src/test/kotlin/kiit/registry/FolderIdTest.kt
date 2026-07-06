package kiit.registry

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FolderIdTest {

    @Test
    fun `folder registers a declaration when the directory exists`() {
        val srcRoot = Files.createTempDirectory("folder-test").toFile()
        Files.createDirectories(File(srcRoot, "models").toPath())

        val registry = Registry()
        RegistryScope(registry).folder(ResourceKind.Data, "entities", "/models", srcRoot = srcRoot.path)

        val folders = registry.inspector.getAllFolders()
        assertEquals(1, folders.size)
        assertEquals(FolderId(ResourceKind.Data, "entities", "/models"), folders[0])
    }

    @Test
    fun `folder throws when the directory does not exist`() {
        val srcRoot = Files.createTempDirectory("folder-test").toFile()
        val registry = Registry()

        assertFailsWith<IllegalStateException> {
            RegistryScope(registry).folder(ResourceKind.Data, "entities", "/does-not-exist", srcRoot = srcRoot.path)
        }
    }

    @Test
    fun `folder declarations from different modules sharing kind and category both remain retrievable, distinguished by module`() {
        val srcRoot = Files.createTempDirectory("folder-test").toFile()
        Files.createDirectories(File(srcRoot, "shared-models").toPath())
        Files.createDirectories(File(srcRoot, "spaces-models").toPath())

        val registry = Registry()
        fun fakeModule(moduleName: String, path: String) = object : Module {
            override val name: String = moduleName
            override val registry: Registry = registry
            override fun register() {
                register { folder(ResourceKind.Data, "entities", path, srcRoot = srcRoot.path) }
            }
        }

        registry.register(listOf(fakeModule("shared", "/shared-models"), fakeModule("spaces", "/spaces-models")))

        val folders = registry.inspector.findFoldersByKindCategory(ResourceKind.Data, "entities")
        assertEquals(2, folders.size)
        assertTrue(folders.any { it.module == "shared" })
        assertTrue(folders.any { it.module == "spaces" })
    }

    @Test
    fun `folder declarations are tagged with the registering module`() {
        val srcRoot = Files.createTempDirectory("folder-test").toFile()
        Files.createDirectories(File(srcRoot, "models").toPath())

        val registry = Registry()
        val fakeModule = object : Module {
            override val name: String = "fake"
            override val registry: Registry = registry
            override fun register() {
                register { folder(ResourceKind.Data, "entities", "/models", srcRoot = srcRoot.path) }
            }
        }

        registry.register(listOf(fakeModule))

        assertEquals("fake", registry.inspector.getAllFolders().single().module)
    }
}
