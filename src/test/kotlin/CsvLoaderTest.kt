import java.io.File
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CsvLoaderTest {

    private fun withTempFile(content: String, block: (String) -> Unit) {
        val file = File.createTempFile("travels_test", ".csv")
        try {
            file.writeText(content)
            block(file.absolutePath)
        } finally {
            file.delete()
        }
    }

    @Test
    fun `valid CSV is parsed correctly`() {
        val csv = """
            start,end,location
            2020-08-10,2020-08-25,TR
            2021-03-15,2021-03-22,UK
        """.trimIndent()

        withTempFile(csv) { path ->
            val result = loadTravelsFromCsv(path)
            assertEquals(2, result.size)
            assertEquals("TR", result[0].location)
            assertEquals(LocalDateTime.of(2020, 8, 10, 0, 0), result[0].start)
            assertEquals(LocalDateTime.of(2020, 8, 25, 0, 0), result[0].end)
            assertEquals("UK", result[1].location)
        }
    }

    @Test
    fun `header-only CSV returns empty list`() {
        val csv = "start,end,location\n"

        withTempFile(csv) { path ->
            val result = loadTravelsFromCsv(path)
            assertEquals(0, result.size)
        }
    }

    @Test
    fun `blank lines are skipped`() {
        val csv = "start,end,location\n2020-08-10,2020-08-25,TR\n\n2021-03-15,2021-03-22,UK\n"

        withTempFile(csv) { path ->
            val result = loadTravelsFromCsv(path)
            assertEquals(2, result.size)
        }
    }

    @Test
    fun `missing file throws`() {
        assertFailsWith<IllegalArgumentException>("Travels CSV file not found") {
            loadTravelsFromCsv("/nonexistent/file.csv")
        }
    }

    @Test
    fun `empty file throws`() {
        withTempFile("") { path ->
            assertFailsWith<IllegalArgumentException>("Travels CSV file is empty") {
                loadTravelsFromCsv(path)
            }
        }
    }

    @Test
    fun `wrong header throws`() {
        val csv = "date,place\n2020-08-10,TR\n"

        withTempFile(csv) { path ->
            assertFailsWith<IllegalArgumentException>("Unexpected CSV header") {
                loadTravelsFromCsv(path)
            }
        }
    }

    @Test
    fun `row with wrong column count throws`() {
        val csv = "start,end,location\n2020-08-10,TR\n"

        withTempFile(csv) { path ->
            assertFailsWith<IllegalArgumentException>("expected 3") {
                loadTravelsFromCsv(path)
            }
        }
    }

    @Test
    fun `empty location throws`() {
        val csv = "start,end,location\n2020-08-10,2020-08-25, \n"

        withTempFile(csv) { path ->
            assertFailsWith<IllegalArgumentException>("empty location") {
                loadTravelsFromCsv(path)
            }
        }
    }

    @Test
    fun `invalid location characters throws`() {
        val csv = "start,end,location\n2020-08-10,2020-08-25,<script>\n"

        withTempFile(csv) { path ->
            assertFailsWith<IllegalArgumentException>("invalid location") {
                loadTravelsFromCsv(path)
            }
        }
    }

    @Test
    fun `end date before start date throws`() {
        val csv = "start,end,location\n2020-08-25,2020-08-10,TR\n"

        withTempFile(csv) { path ->
            assertFailsWith<IllegalArgumentException>("end date before start date") {
                loadTravelsFromCsv(path)
            }
        }
    }

    @Test
    fun `invalid date format throws`() {
        val csv = "start,end,location\n10-08-2020,25-08-2020,TR\n"

        withTempFile(csv) { path ->
            assertFailsWith<IllegalArgumentException>("Invalid date format") {
                loadTravelsFromCsv(path)
            }
        }
    }
}
