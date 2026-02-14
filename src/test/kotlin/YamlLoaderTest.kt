import java.io.File
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class YamlLoaderTest {

    private fun withTempFile(content: String, block: (String) -> Unit) {
        val file = File.createTempFile("irp_test", ".yaml")
        try {
            file.writeText(content)
            block(file.absolutePath)
        } finally {
            file.delete()
        }
    }

    @Test
    fun `valid YAML is parsed correctly`() {
        val yaml = """
            ireland_first_entry: 2020-01-15
            irp:
              - name: stamp1
                start: 2020-07-01
                end: 2021-07-01
              - name: stamp4
                start: 2021-06-25
                end: 2025-06-25
        """.trimIndent()

        withTempFile(yaml) { path ->
            val result = loadIrpInfo(path)
            assertEquals(LocalDateTime.of(2020, 1, 15, 0, 0), result.irelandFirstEntry)
            assertEquals(2, result.irpEntries.size)
            assertEquals("stamp1", result.irpEntries[0].name)
            assertEquals(LocalDateTime.of(2020, 7, 1, 0, 0), result.irpEntries[0].start)
            assertEquals(LocalDateTime.of(2021, 7, 1, 0, 0), result.irpEntries[0].end)
            assertEquals("stamp4", result.irpEntries[1].name)
        }
    }

    @Test
    fun `missing file throws`() {
        assertFailsWith<IllegalArgumentException>("IRP info file not found") {
            loadIrpInfo("/nonexistent/path.yaml")
        }
    }

    @Test
    fun `missing ireland_first_entry throws`() {
        val yaml = """
            irp:
              - name: stamp1
                start: 2020-07-01
                end: 2021-07-01
        """.trimIndent()

        withTempFile(yaml) { path ->
            assertFailsWith<IllegalArgumentException>("Missing required key 'ireland_first_entry'") {
                loadIrpInfo(path)
            }
        }
    }

    @Test
    fun `missing irp key throws`() {
        val yaml = """
            ireland_first_entry: 2020-01-15
        """.trimIndent()

        withTempFile(yaml) { path ->
            assertFailsWith<IllegalArgumentException>("Missing required key 'irp'") {
                loadIrpInfo(path)
            }
        }
    }

    @Test
    fun `empty irp list throws`() {
        val yaml = """
            ireland_first_entry: 2020-01-15
            irp: []
        """.trimIndent()

        withTempFile(yaml) { path ->
            assertFailsWith<IllegalArgumentException>("'irp' list must not be empty") {
                loadIrpInfo(path)
            }
        }
    }

    @Test
    fun `unexpected top-level key throws`() {
        val yaml = """
            ireland_first_entry: 2020-01-15
            irp:
              - name: stamp1
                start: 2020-07-01
                end: 2021-07-01
            extra_key: bad
        """.trimIndent()

        withTempFile(yaml) { path ->
            assertFailsWith<IllegalArgumentException>("Unexpected keys") {
                loadIrpInfo(path)
            }
        }
    }

    @Test
    fun `unexpected key in irp entry throws`() {
        val yaml = """
            ireland_first_entry: 2020-01-15
            irp:
              - name: stamp1
                start: 2020-07-01
                end: 2021-07-01
                extra: bad
        """.trimIndent()

        withTempFile(yaml) { path ->
            assertFailsWith<IllegalArgumentException>("Unexpected keys in IRP entry") {
                loadIrpInfo(path)
            }
        }
    }

    @Test
    fun `irp entry missing name throws`() {
        val yaml = """
            ireland_first_entry: 2020-01-15
            irp:
              - start: 2020-07-01
                end: 2021-07-01
        """.trimIndent()

        withTempFile(yaml) { path ->
            assertFailsWith<IllegalArgumentException>("missing 'name'") {
                loadIrpInfo(path)
            }
        }
    }

    @Test
    fun `irp entry with invalid name characters throws`() {
        val yaml = """
            ireland_first_entry: 2020-01-15
            irp:
              - name: "stamp<script>"
                start: 2020-07-01
                end: 2021-07-01
        """.trimIndent()

        withTempFile(yaml) { path ->
            assertFailsWith<IllegalArgumentException>("invalid characters") {
                loadIrpInfo(path)
            }
        }
    }

    @Test
    fun `irp entry with end before start throws`() {
        val yaml = """
            ireland_first_entry: 2020-01-15
            irp:
              - name: stamp1
                start: 2021-07-01
                end: 2020-07-01
        """.trimIndent()

        withTempFile(yaml) { path ->
            assertFailsWith<IllegalArgumentException>("end date before start date") {
                loadIrpInfo(path)
            }
        }
    }

    @Test
    fun `invalid date format throws`() {
        val yaml = """
            ireland_first_entry: 15-01-2020
            irp:
              - name: stamp1
                start: 2020-07-01
                end: 2021-07-01
        """.trimIndent()

        withTempFile(yaml) { path ->
            assertFailsWith<IllegalArgumentException>("Invalid date format") {
                loadIrpInfo(path)
            }
        }
    }

    @Test
    fun `non-mapping top level throws`() {
        val yaml = "- item1\n- item2"

        withTempFile(yaml) { path ->
            assertFailsWith<IllegalArgumentException>("YAML mapping at the top level") {
                loadIrpInfo(path)
            }
        }
    }
}
