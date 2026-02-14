import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class ReportGeneratorTest {

    private fun dt(year: Int, month: Int, day: Int): LocalDateTime =
        LocalDateTime.of(year, month, day, 0, 0)

    private fun zdt(year: Int, month: Int, day: Int): ZonedDateTime =
        dt(year, month, day).atZone(ZoneId.of("GMT"))

    @Test
    fun `generateReport contains expected sections`() {
        val today = zdt(2024, 6, 1)
        val todayLocal = dt(2024, 6, 1)
        val irelandFirstEntry = dt(2020, 1, 15)
        val irpEntries = listOf(
            IrpEntry("stamp1", dt(2020, 7, 1), dt(2021, 7, 1)),
            IrpEntry("stamp4", dt(2021, 6, 25), dt(2025, 6, 25))
        )
        val travels = listOf(
            Travel(dt(2020, 8, 10), dt(2020, 8, 25), "TR")
        )

        val report = generateReport(
            today = today,
            todayLocalDate = todayLocal,
            irelandFirstEntry = irelandFirstEntry,
            irpEntries = irpEntries,
            travelsList = travels,
            daysWithoutIRP = 168,
            irpDays = 1437,
            lastIrpEnd = dt(2025, 6, 25),
            absentDays = 14,
            daysAccumulated = 1605,
            daysLeft = 235,
            dayToApply = today.plusDays(235),
            goalDays = 1826,
            excuseDays = 70
        )

        assert(report.contains("# Reckonable Residence Report"))
        assert(report.contains("## IRP Entries"))
        assert(report.contains("stamp1"))
        assert(report.contains("stamp4"))
        assert(report.contains("## Travel History"))
        assert(report.contains("TR"))
        assert(report.contains("## Year-by-Year Absence Breakdown"))
        assert(report.contains("## Summary"))
        assert(report.contains("1826 days"))
        assert(report.contains("## Key Dates"))
        assert(report.contains("Earliest Application Date"))
    }

    @Test
    fun `escapeMd escapes pipe characters`() {
        assertEquals("foo\\|bar", escapeMd("foo|bar"))
    }

    @Test
    fun `escapeMd escapes brackets`() {
        assertEquals("\\[link\\]\\(url\\)", escapeMd("[link](url)"))
    }

    @Test
    fun `escapeMd escapes angle brackets`() {
        assertEquals("&lt;script&gt;", escapeMd("<script>"))
    }

    @Test
    fun `escapeMd escapes markdown emphasis`() {
        assertEquals("\\*bold\\*", escapeMd("*bold*"))
        assertEquals("\\_italic\\_", escapeMd("_italic_"))
    }

    @Test
    fun `escapeMd escapes backslashes`() {
        assertEquals("\\\\n", escapeMd("\\n"))
    }

    @Test
    fun `escapeMd with plain text is unchanged`() {
        assertEquals("stamp4", escapeMd("stamp4"))
        assertEquals("hello world", escapeMd("hello world"))
    }
}
